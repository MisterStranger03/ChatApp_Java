package chatting;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLDatabase {

    public static final String DB_URL = "jdbc:sqlite:chatapp.db";
    public Connection conn;

    public SQLDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(DB_URL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initialize() {
        String createUsers = "CREATE TABLE IF NOT EXISTS users (" +
                "username TEXT PRIMARY KEY, name TEXT, password TEXT, profile_photo TEXT)";
        String createMessages = "CREATE TABLE IF NOT EXISTS messages (" +
                "message_id TEXT PRIMARY KEY, sender TEXT, recipient TEXT, content TEXT, type TEXT, file_data TEXT, status TEXT, timestamp INTEGER)";
        String createFriends = "CREATE TABLE IF NOT EXISTS friends (" +
                "user TEXT, friend TEXT, " +
                "PRIMARY KEY (user, friend), " +
                "FOREIGN KEY (user) REFERENCES users(username), " +
                "FOREIGN KEY (friend) REFERENCES users(username))";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createUsers);
            stmt.execute(createMessages);
            stmt.execute(createFriends);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean addFriend(String username, String friendUsername) {
        String query = "INSERT OR IGNORE INTO friends (user, friend) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, username);
            ps.setString(2, friendUsername);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0; // Return true if insertion was successful
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    

    public boolean removeFriend(String username, String friendUsername) {
        String query = "DELETE FROM friends WHERE user = ? AND friend = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, username);
            ps.setString(2, friendUsername);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0; // Return true if deletion was successful
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    

    public List<String> getFriends(String username) {
        List<String> friendsList = new ArrayList<>();
        String query = "SELECT friend FROM friends WHERE user = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String friend = rs.getString("friend");
                System.out.println(username + " has friend: " + friend);
                friendsList.add(friend);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return friendsList;
    }
    

    public boolean isFriend(String username, String friendUsername) {
        String query = "SELECT COUNT(*) FROM friends WHERE user = ? AND friend = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, username);
            ps.setString(2, friendUsername);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Map<String, UserData.User> loadUsers() {
        Map<String, UserData.User> userMap = new HashMap<>();
        String query = "SELECT * FROM users";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String username = rs.getString("username");
                String name = rs.getString("name");
                String password = rs.getString("password");
                String photo = rs.getString("profile_photo");
                UserData.User u = new UserData.User(name, username, password);
                u.setProfilePhotoBase64(photo);
                userMap.put(username, u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userMap;
    }

    public void saveUser(UserData.User u) {
        String insert = "INSERT OR REPLACE INTO users(username, name, password, profile_photo) VALUES (?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insert)) {
            pstmt.setString(1, u.getUsername());
            pstmt.setString(2, u.getName());
            pstmt.setString(3, u.getPassword());
            pstmt.setString(4, u.getProfilePhotoBase64());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateUser(UserData.User u) {
        saveUser(u);
    }

    public void saveMessage(MessageData.Message m) {
        String insert = "INSERT OR REPLACE INTO messages(message_id, sender, recipient, content, type, file_data, status, timestamp) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insert)) {
            pstmt.setString(1, m.getMessageId());
            pstmt.setString(2, m.getSender());
            pstmt.setString(3, m.getRecipient());
            pstmt.setString(4, m.getContent());
            pstmt.setString(5, m.getType());
            pstmt.setString(6, m.getFileData());
            pstmt.setString(7, m.getStatus());
            pstmt.setLong(8, m.getTimestamp());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveMessagesForUser(String username, Map<String, List<MessageData.Message>> chatHistory) {
        String delete = "DELETE FROM messages WHERE sender = ? OR recipient = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(delete)) {
            pstmt.setString(1, username);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        for (List<MessageData.Message> msgs : chatHistory.values()) {
            for (MessageData.Message m : msgs) {
                saveMessage(m);
            }
        }
    }

    public Map<String, List<MessageData.Message>> loadMessagesForUser(String username) {
        Map<String, List<MessageData.Message>> history = new HashMap<>();
        String query = "SELECT * FROM messages WHERE sender = ? OR recipient = ? OR type LIKE 'GROUP_%' ORDER BY timestamp ASC";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String msgId = rs.getString("message_id");
                    String sender = rs.getString("sender");
                    String recipient = rs.getString("recipient");
                    String content = rs.getString("content");
                    String type = rs.getString("type");
                    String fileData = rs.getString("file_data");
                    String status = rs.getString("status");
                    long timestamp = rs.getLong("timestamp");
                    MessageData.Message m = new MessageData.Message(msgId, sender, recipient, content, type, fileData);
                    m.setStatus(status);
                    m.setTimestamp(timestamp);
                    String key;
                    if (type.startsWith("GROUP_")) {
                        key = "Group:" + recipient;
                    } else {
                        key = sender.equals(username) ? recipient : sender;
                    }
                    history.computeIfAbsent(key, k -> new ArrayList<>()).add(m);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }
}