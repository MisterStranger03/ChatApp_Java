import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 12345;
    // Map username -> ClientHandler (thread-safe)
    private Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        new ChatServer().startServer();
    }

    public void startServer() {
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

                String message;
                while ((message = in.readLine()) != null) {
                    // Protocol: TYPE|messageId|sender|recipient|content|[optional fileData]
                    String[] parts = message.split("\\|", 6);
                    if (parts.length < 1)
                        continue;
                    String type = parts[0];
                    if (type.equals("MSG") || type.equals("FILE")) {
                        String msgId = parts[1];
                        String sender = parts[2];
                        String recipient = parts[3];
                        // Forward the entire message to the recipient if connected.
                        ClientHandler recipientHandler = clients.get(recipient);
                        if (recipientHandler != null) {
                            recipientHandler.out.println(message);
                            // Send DELIVERED ACK to sender.
                            ClientHandler senderHandler = clients.get(sender);
                            if (senderHandler != null) {
                                senderHandler.out.println("ACK|" + msgId + "|DELIVERED");
                            }
                        } else {
                            // If recipient not connected, send FAILED ACK.
                            ClientHandler senderHandler = clients.get(sender);
                            if (senderHandler != null) {
                                senderHandler.out.println("ACK|" + msgId + "|FAILED");
                            }
                        }
                    } else if (type.equals("ACK")) {
                        // For ACK, simply broadcast to all (or you can target the original sender).
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
