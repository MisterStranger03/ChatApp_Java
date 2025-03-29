import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 12345;
    // Map username -> ClientHandler (thread-safe)
    private Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    // In-memory groups map, loaded from DB and updated as needed
    private Map<String, Set<String>> groups = new ConcurrentHashMap<>();
    private GroupDatabase groupDB = new GroupDatabase();

    public static void main(String[] args) {
        new ChatServer().startServer();
    }

    public void startServer() {
        groups = groupDB.loadGroups();
        System.out.println("Loaded groups: " + groups);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("ChatServer started on port " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper class to persist groups in the SQLite database.
    private class GroupDatabase {
        private final String DB_URL = "jdbc:sqlite:chatapp.db";

        public GroupDatabase() {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (Exception e) {
                e.printStackTrace();
            }
            initialize();
        }

        public void initialize() {
            String sql = "CREATE TABLE IF NOT EXISTS groups (" +
                    "group_name TEXT PRIMARY KEY, " +
                    "members TEXT" +
                    ")";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                    Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public Map<String, Set<String>> loadGroups() {
            Map<String, Set<String>> groupMap = new HashMap<>();
            String sql = "SELECT * FROM groups";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String groupName = rs.getString("group_name");
                    String membersStr = rs.getString("members");
                    Set<String> members = new HashSet<>(Arrays.asList(membersStr.split(",")));
                    groupMap.put(groupName, members);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return groupMap;
        }

        public void saveGroup(String groupName, Set<String> members) {
            String sql = "INSERT OR REPLACE INTO groups(group_name, members) VALUES(?,?)";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                    PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, groupName);
                pstmt.setString(2, String.join(",", members));
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public void deleteGroup(String groupName) {
            String sql = "DELETE FROM groups WHERE group_name = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                    PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, groupName);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // First message is the username for identification.
                username = in.readLine();
                if (username == null)
                    return;
                clients.put(username, this);
                System.out.println(username + " connected.");

                // Send groups the user belongs to so that the client can add these groups
                // locally.
                for (String groupName : groups.keySet()) {
                    Set<String> mem = groups.get(groupName);
                    if (mem.contains(username)) {
                        out.println("GROUP_CREATED|" + groupName + "|" + String.join(",", mem));
                    }
                }

                String message;
                while ((message = in.readLine()) != null) {
                    // Protocol: TYPE|... (varies by command)
                    String[] parts = message.split("\\|", 7);
                    if (parts.length < 1)
                        continue;
                    String type = parts[0];

                    // --- Direct messaging or file transfer ---
                    if (type.equals("MSG") || type.equals("FILE")) {
                        if (parts.length < 5)
                            continue;
                        String msgId = parts[1];
                        String sender = parts[2];
                        String recipient = parts[3];
                        ClientHandler recipientHandler = clients.get(recipient);
                        if (recipientHandler != null) {
                            recipientHandler.out.println(message);
                            ClientHandler senderHandler = clients.get(sender);
                            if (senderHandler != null) {
                                senderHandler.out.println("ACK|" + msgId + "|DELIVERED");
                            }
                        } else {
                            ClientHandler senderHandler = clients.get(sender);
                            if (senderHandler != null) {
                                senderHandler.out.println("ACK|" + msgId + "|FAILED");
                            }
                        }
                    }
                    // --- Group chat creation ---
                    else if (type.equals("CREATE_GROUP")) {
                        // Format: CREATE_GROUP|groupName|creator|user1,user2,...
                        if (parts.length < 4)
                            continue;
                        String groupName = parts[1];
                        String creator = parts[2];
                        String membersStr = parts[3];
                        Set<String> groupMembers = new HashSet<>(Arrays.asList(membersStr.split(",")));
                        groupMembers.add(creator); // Ensure creator is included
                        groups.put(groupName, groupMembers);
                        groupDB.saveGroup(groupName, groupMembers);
                        System.out.println("Group created: " + groupName + " with members " + groupMembers);
                        // Notify all connected group members
                        for (String member : groupMembers) {
                            ClientHandler handler = clients.get(member);
                            if (handler != null) {
                                handler.out.println("GROUP_CREATED|" + groupName + "|" + membersStr);
                            }
                        }
                    }
                    // --- Group messaging ---
                    else if (type.equals("GROUP_MSG")) {
                        // Format: GROUP_MSG|msgId|sender|groupName|content
                        if (parts.length < 5)
                            continue;
                        String msgId = parts[1];
                        String sender = parts[2];
                        String groupName = parts[3];
                        String content = parts[4];
                        Set<String> members = groups.get(groupName);
                        if (members != null) {
                            for (String member : members) {
                                if (!member.equals(sender)) {
                                    ClientHandler memberHandler = clients.get(member);
                                    if (memberHandler != null) {
                                        memberHandler.out.println(
                                                "GROUP_MSG|" + msgId + "|" + sender + "|" + groupName + "|" + content);
                                    }
                                }
                            }
                            ClientHandler senderHandler = clients.get(sender);
                            if (senderHandler != null) {
                                senderHandler.out.println("ACK|" + msgId + "|DELIVERED");
                            }
                        } else {
                            ClientHandler senderHandler = clients.get(sender);
                            if (senderHandler != null) {
                                senderHandler.out.println("ACK|" + msgId + "|FAILED");
                            }
                        }
                    }
                    // --- Group file transfer ---
                    else if (type.equals("GROUP_FILE")) {
                        // Format: GROUP_FILE|msgId|sender|groupName|filename|base64data
                        if (parts.length < 6)
                            continue;
                        String msgId = parts[1];
                        String sender = parts[2];
                        String groupName = parts[3];
                        String filename = parts[4];
                        String base64data = parts[5];
                        Set<String> members = groups.get(groupName);
                        if (members != null) {
                            for (String member : members) {
                                if (!member.equals(sender)) {
                                    ClientHandler memberHandler = clients.get(member);
                                    if (memberHandler != null) {
                                        memberHandler.out.println("GROUP_FILE|" + msgId + "|" + sender + "|" + groupName
                                                + "|" + filename + "|" + base64data);
                                    }
                                }
                            }
                            ClientHandler senderHandler = clients.get(sender);
                            if (senderHandler != null) {
                                senderHandler.out.println("ACK|" + msgId + "|DELIVERED");
                            }
                        } else {
                            ClientHandler senderHandler = clients.get(sender);
                            if (senderHandler != null) {
                                senderHandler.out.println("ACK|" + msgId + "|FAILED");
                            }
                        }
                    }
                    // --- Leave group ---
                    else if (type.equals("LEAVE_GROUP")) {
                        // Format: LEAVE_GROUP|groupName|username
                        if (parts.length < 3)
                            continue;
                        String groupName = parts[1];
                        String user = parts[2];
                        Set<String> members = groups.get(groupName);
                        if (members != null) {
                            members.remove(user);
                            System.out.println(user + " left group " + groupName);
                            for (String member : members) {
                                ClientHandler memberHandler = clients.get(member);
                                if (memberHandler != null) {
                                    memberHandler.out.println("GROUP_UPDATE|" + groupName + "|MEMBER_LEFT|" + user);
                                }
                            }
                            if (members.isEmpty()) {
                                groups.remove(groupName);
                                groupDB.deleteGroup(groupName);
                            } else {
                                groupDB.saveGroup(groupName, members);
                            }
                        }
                    }
                    // --- Update group name ---
                    else if (type.equals("UPDATE_GROUP")) {
                        // Format: UPDATE_GROUP|oldGroupName|newGroupName|username
                        if (parts.length < 4)
                            continue;
                        String oldGroupName = parts[1];
                        String newGroupName = parts[2];
                        String user = parts[3];
                        Set<String> members = groups.get(oldGroupName);
                        if (members != null && members.contains(user)) {
                            groups.remove(oldGroupName);
                            groups.put(newGroupName, members);
                            System.out
                                    .println(user + " changed group name from " + oldGroupName + " to " + newGroupName);
                            for (String member : members) {
                                ClientHandler memberHandler = clients.get(member);
                                if (memberHandler != null) {
                                    memberHandler.out
                                            .println("GROUP_UPDATE|" + oldGroupName + "|NAME_CHANGED|" + newGroupName);
                                }
                            }
                            groupDB.deleteGroup(oldGroupName);
                            groupDB.saveGroup(newGroupName, members);
                        }
                    }
                    // --- Add user to group ---
                    else if (type.equals("ADD_TO_GROUP")) {
                        // Format: ADD_TO_GROUP|groupName|adder|newUser
                        if (parts.length < 4)
                            continue;
                        String groupName = parts[1];
                        String adder = parts[2];
                        String newUser = parts[3];
                        Set<String> members = groups.get(groupName);
                        if (members != null && members.contains(adder)) {
                            members.add(newUser);
                            System.out.println(adder + " added " + newUser + " to group " + groupName);
                            for (String member : members) {
                                ClientHandler memberHandler = clients.get(member);
                                if (memberHandler != null) {
                                    memberHandler.out.println("GROUP_UPDATE|" + groupName + "|USER_ADDED|" + newUser);
                                }
                            }
                            groupDB.saveGroup(groupName, members);
                        }
                    }
                    // --- Request group info ---
                    else if (type.equals("GROUP_INFO")) {
                        // Format: GROUP_INFO|groupName
                        if (parts.length < 2)
                            continue;
                        String groupName = parts[1];
                        Set<String> members = groups.get(groupName);
                        if (members != null) {
                            out.println("GROUP_INFO|" + groupName + "|" + String.join(",", members));
                        }
                    }
                    // --- ACK handling ---
                    else if (type.equals("ACK")) {
                        for (ClientHandler handler : clients.values()) {
                            if (!handler.username.equals(username)) {
                                handler.out.println(message);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (username != null) {
                    clients.remove(username);
                    System.out.println(username + " disconnected.");
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
