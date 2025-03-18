import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.*;

public class ChatClient extends JFrame {
    // Persistent storage file for users and chat history.
    private static final String DATA_FILE = "chatapp_data.ser";
    // Map of registered users (username -> User)
    private Map<String, User> users;
    // Currently logged in user.
    private User currentUser;

    // CardLayout for switching screens (login, register, chat).
    private CardLayout cardLayout;
    private JPanel mainPanel;

    // Network client for real-time messaging.
    private NetworkClient networkClient;
    private final String SERVER_ADDRESS = "localhost";
    private final int SERVER_PORT = 12345;

    // Reference to ChatMainPanel to update conversation panels.
    private ChatMainPanel chatMainPanel;

    // For generating unique message IDs.
    private AtomicLong messageIdGenerator = new AtomicLong(System.currentTimeMillis());

    public ChatClient() {
        setTitle("Modern Chat App - Chat Client");
        setSize(900, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        loadData();

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(new LoginPanel(), "login");
        mainPanel.add(new RegistrationPanel(), "register");
        chatMainPanel = new ChatMainPanel();
        mainPanel.add(chatMainPanel, "chat");

        add(mainPanel);
        cardLayout.show(mainPanel, "login");
    }

    // Load persistent user data.
    @SuppressWarnings("unchecked")
    private void loadData() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                users = (Map<String, User>) ois.readObject();
            } catch (Exception e) {
                users = new HashMap<>();
            }
        } else {
            users = new HashMap<>();
        }
    }

    // Save persistent user data.
    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving data: " + e.getMessage());
        }
    }

    // User data model (must be Serializable).
    private static class User implements Serializable {
        private static final long serialVersionUID = 1L;
        String name;
        String username;
        String password;
        // Chat history: contact -> list of Message objects.
        Map<String, List<Message>> chatHistory = new HashMap<>();
        // Unread counts: contact -> count of unread messages.
        Map<String, Integer> unreadCounts = new HashMap<>();
        // Unread snippets: contact -> snippet text.
        Map<String, String> unreadSnippets = new HashMap<>();

        User(String name, String username, String password) {
            this.name = name;
            this.username = username;
            this.password = password;
        }
    }

    // Login Panel.
    private class LoginPanel extends JPanel {
        public LoginPanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(245, 245, 245));

            JLabel title = new JLabel("Login", SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 32));
            title.setForeground(new Color(30, 144, 255));
            add(title, BorderLayout.NORTH);

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBackground(new Color(245, 245, 245));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0;
            gbc.gridy = 0;
            formPanel.add(new JLabel("Username:"), gbc);
            gbc.gridx = 1;
            JTextField usernameField = new JTextField(20);
            formPanel.add(usernameField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            formPanel.add(new JLabel("Password:"), gbc);
            gbc.gridx = 1;
            JPasswordField passwordField = new JPasswordField(20);
            formPanel.add(passwordField, gbc);

            JButton loginButton = new JButton("Login");
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 2;
            formPanel.add(loginButton, gbc);

            JButton toRegisterButton = new JButton("Register");
            gbc.gridy = 3;
            formPanel.add(toRegisterButton, gbc);

            add(formPanel, BorderLayout.CENTER);

            // Action Listeners.
            loginButton.addActionListener(e -> {
                String username = usernameField.getText().trim();
                String password = new String(passwordField.getPassword());
                if (username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please fill all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (users.containsKey(username)) {
                    User user = users.get(username);
                    if (user.password.equals(password)) {
                        currentUser = user;
                        chatMainPanel.refreshContacts();
                        chatMainPanel.refreshChatHistory();
                        networkClient = new NetworkClient(currentUser.username);
                        new Thread(networkClient).start();
                        cardLayout.show(mainPanel, "chat");
                    } else {
                        JOptionPane.showMessageDialog(this, "Incorrect password.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "User not found.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            toRegisterButton.addActionListener(e -> cardLayout.show(mainPanel, "register"));
        }
    }

    // Registration Panel.
    private class RegistrationPanel extends JPanel {
        public RegistrationPanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(245, 245, 245));

            JLabel title = new JLabel("Register", SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 32));
            title.setForeground(new Color(34, 139, 34));
            add(title, BorderLayout.NORTH);

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBackground(new Color(245, 245, 245));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0;
            gbc.gridy = 0;
            formPanel.add(new JLabel("Name:"), gbc);
            gbc.gridx = 1;
            JTextField nameField = new JTextField(20);
            formPanel.add(nameField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            formPanel.add(new JLabel("Username:"), gbc);
            gbc.gridx = 1;
            JTextField usernameField = new JTextField(20);
            formPanel.add(usernameField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            formPanel.add(new JLabel("Password:"), gbc);
            gbc.gridx = 1;
            JPasswordField passwordField = new JPasswordField(20);
            formPanel.add(passwordField, gbc);

            JButton registerButton = new JButton("Register");
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.gridwidth = 2;
            formPanel.add(registerButton, gbc);

            JButton backToLoginButton = new JButton("Back to Login");
            gbc.gridy = 4;
            formPanel.add(backToLoginButton, gbc);

            add(formPanel, BorderLayout.CENTER);

            registerButton.addActionListener(e -> {
                String name = nameField.getText().trim();
                String username = usernameField.getText().trim();
                String password = new String(passwordField.getPassword());
                if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please fill all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (users.containsKey(username)) {
                    JOptionPane.showMessageDialog(this, "Username already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                User newUser = new User(name, username, password);
                users.put(username, newUser);
                saveData();
                JOptionPane.showMessageDialog(this, "Registration successful! Please login.", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                cardLayout.show(mainPanel, "login");
            });

            backToLoginButton.addActionListener(e -> cardLayout.show(mainPanel, "login"));
        }
    }

    // Main Chat Panel with contacts list, conversation area, logout, refresh, and
    // file sharing.
    private class ChatMainPanel extends JPanel {
        private DefaultListModel<String> contactsModel;
        private JList<String> contactsList;
        private JPanel chatSessionPanel;
        private JLabel headerLabel;
        // To track the currently open conversation.
        private String currentChatContact = null;
        private JPanel conversationPanel; // Panel to hold individual message panels.

        public ChatMainPanel() {
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);

            // Header with welcome message, refresh and logout buttons.
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(new Color(30, 144, 255));
            headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            headerLabel = new JLabel("Welcome, ", SwingConstants.LEFT);
            headerLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
            headerLabel.setForeground(Color.WHITE);
            headerPanel.add(headerLabel, BorderLayout.WEST);

            JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
            eastPanel.setOpaque(false);
            JButton refreshButton = new JButton("Refresh Contacts");
            refreshButton.setFocusPainted(false);
            refreshButton.addActionListener(e -> {
                loadData(); // Reload persistent data.
                refreshContacts();
            });
            JButton logoutButton = new JButton("Logout");
            logoutButton.setFocusPainted(false);
            logoutButton.addActionListener(e -> {
                if (networkClient != null)
                    networkClient.close();
                currentUser = null;
                cardLayout.show(mainPanel, "login");
            });
            eastPanel.add(refreshButton);
            eastPanel.add(logoutButton);
            headerPanel.add(eastPanel, BorderLayout.EAST);
            add(headerPanel, BorderLayout.NORTH);

            // Contacts list on the left, including unread count and snippet.
            contactsModel = new DefaultListModel<>();
            contactsList = new JList<>(contactsModel);
            contactsList.setFont(new Font("SansSerif", Font.PLAIN, 16));
            JScrollPane contactsScroll = new JScrollPane(contactsList);
            contactsScroll.setPreferredSize(new Dimension(250, 0));
            contactsScroll.setBorder(BorderFactory.createTitledBorder("Contacts"));

            // Chat session panel on the right.
            chatSessionPanel = new JPanel(new BorderLayout());
            chatSessionPanel.setBorder(BorderFactory.createTitledBorder("Conversation"));
            chatSessionPanel.setBackground(new Color(250, 250, 250));

            contactsList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        String display = contactsList.getSelectedValue();
                        if (display != null) {
                            // Extract username from display.
                            String contact = display.split(" ")[0];
                            openChatSession(contact);
                        }
                    }
                }
            });

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, contactsScroll, chatSessionPanel);
            add(splitPane, BorderLayout.CENTER);
        }

        public void refreshContacts() {
            headerLabel.setText("Welcome, " + currentUser.name);
            contactsModel.clear();
            for (String uname : users.keySet()) {
                if (!uname.equals(currentUser.username)) {
                    int unread = currentUser.unreadCounts.getOrDefault(uname, 0);
                    String snippet = currentUser.unreadSnippets.getOrDefault(uname, "");
                    String display = uname;
                    if (unread > 0) {
                        display += "  (" + unread + ")";
                        if (!snippet.isEmpty()) {
                            display += " - " + snippet;
                        }
                    }
                    contactsModel.addElement(display);
                }
            }
        }

        public void refreshChatHistory() {
            // Chat history is loaded from currentUser.
        }

        public void openChatSession(String contact) {
            currentChatContact = contact;
            // Clear unread count and snippet for this contact.
            currentUser.unreadCounts.put(contact, 0);
            currentUser.unreadSnippets.remove(contact);
            refreshContacts();

            chatSessionPanel.removeAll();
            chatSessionPanel.setLayout(new BorderLayout());

            // Conversation panel to hold messages.
            conversationPanel = new JPanel();
            conversationPanel.setLayout(new BoxLayout(conversationPanel, BoxLayout.Y_AXIS));
            JScrollPane convScroll = new JScrollPane(conversationPanel);

            // Load conversation history if exists.
            List<Message> history = currentUser.chatHistory.get(contact);
            if (history != null) {
                for (Message m : history) {
                    conversationPanel.add(createMessagePanel(m));
                }
            }

            // Input panel with text field, Send, Send File, and Close Chat buttons.
            JTextField inputField = new JTextField();
            inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
            JButton sendMsgButton = new JButton("Send");
            JButton sendFileButton = new JButton("Send File");
            JButton closeChatButton = new JButton("Close Chat");
            JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
            inputPanel.add(inputField);
            inputPanel.add(sendMsgButton);
            inputPanel.add(sendFileButton);
            inputPanel.add(closeChatButton);

            sendMsgButton.addActionListener(e -> {
                String msgText = inputField.getText().trim();
                if (!msgText.isEmpty()) {
                    String msgId = currentUser.username + "-" + messageIdGenerator.getAndIncrement();
                    Message msg = new Message(msgId, currentUser.username, contact, msgText, "MSG", null);
                    msg.setStatus("PENDING");
                    addMessageToHistory(contact, msg);
                    conversationPanel.add(createMessagePanel(msg));
                    conversationPanel.revalidate();
                    conversationPanel.repaint();
                    inputField.setText("");
                    if (networkClient != null) {
                        networkClient.sendMessage(contact,
                                "MSG|" + msgId + "|" + currentUser.username + "|" + contact + "|" + msgText);
                    }
                }
            });

            sendFileButton.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                int res = fileChooser.showOpenDialog(ChatMainPanel.this);
                if (res == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    try {
                        byte[] fileBytes = Files.readAllBytes(file.toPath());
                        String base64Encoded = Base64.getEncoder().encodeToString(fileBytes);
                        String msgId = currentUser.username + "-" + messageIdGenerator.getAndIncrement();
                        // The content field will store the file name.
                        Message fileMsg = new Message(msgId, currentUser.username, contact, file.getName(), "FILE",
                                base64Encoded);
                        fileMsg.setStatus("PENDING");
                        addMessageToHistory(contact, fileMsg);
                        conversationPanel.add(createMessagePanel(fileMsg));
                        conversationPanel.revalidate();
                        conversationPanel.repaint();
                        if (networkClient != null) {
                            networkClient.sendMessage(contact, "FILE|" + msgId + "|" + currentUser.username + "|"
                                    + contact + "|" + file.getName() + "|" + base64Encoded);
                        }
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(ChatMainPanel.this, "Error reading file: " + ex.getMessage());
                    }
                }
            });

            closeChatButton.addActionListener(e -> {
                chatSessionPanel.removeAll();
                chatSessionPanel.revalidate();
                chatSessionPanel.repaint();
                currentChatContact = null;
            });

            chatSessionPanel.add(convScroll, BorderLayout.CENTER);
            chatSessionPanel.add(inputPanel, BorderLayout.SOUTH);
            chatSessionPanel.revalidate();
            chatSessionPanel.repaint();

            // Mark messages as read.
            if (currentUser.chatHistory.containsKey(contact)) {
                for (Message m : currentUser.chatHistory.get(contact)) {
                    if (!"READ".equals(m.getStatus()) && m.getSender().equals(contact)) {
                        m.setStatus("READ");
                        if (networkClient != null) {
                            networkClient.sendMessage(contact, "ACK|" + m.getMessageId() + "|READ");
                        }
                    }
                }
                saveData();
            }
        }

        // Create a JPanel to represent a single message.
        private JPanel createMessagePanel(Message m) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            JLabel msgLabel;
            if (m.getType().equals("MSG")) {
                msgLabel = new JLabel(m.getSender() + ": " + m.getContent());
            } else if (m.getType().equals("FILE")) {
                msgLabel = new JLabel(m.getSender() + " sent a file: " + m.getContent());
            } else {
                msgLabel = new JLabel(m.getSender() + ": " + m.getContent());
            }
            panel.add(msgLabel, BorderLayout.CENTER);
            // Status label for delivery/read ticks.
            JLabel statusLabel = new JLabel();
            if (m.getSender().equals(currentUser.username)) {
                if ("DELIVERED".equals(m.getStatus()))
                    statusLabel.setText(" ✔");
                else if ("READ".equals(m.getStatus()))
                    statusLabel.setText(" ✔✔");
                else
                    statusLabel.setText(" …");
            }
            panel.add(statusLabel, BorderLayout.EAST);
            // For file messages, add a Download button.
            if (m.getType().equals("FILE")) {
                JButton downloadBtn = new JButton("Download");
                downloadBtn.addActionListener(e -> {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setSelectedFile(new File(m.getContent()));
                    int res = chooser.showSaveDialog(ChatMainPanel.this);
                    if (res == JFileChooser.APPROVE_OPTION) {
                        File outFile = chooser.getSelectedFile();
                        try {
                            byte[] data = Base64.getDecoder().decode(m.getFileData());
                            Files.write(outFile.toPath(), data);
                            JOptionPane.showMessageDialog(ChatMainPanel.this,
                                    "File downloaded to " + outFile.getAbsolutePath());
                            downloadBtn.setEnabled(false);
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(ChatMainPanel.this, "Error saving file: " + ex.getMessage());
                        }
                    }
                });
                panel.add(downloadBtn, BorderLayout.SOUTH);
            }
            return panel;
        }

        private void addMessageToHistory(String contact, Message m) {
            currentUser.chatHistory.computeIfAbsent(contact, k -> new ArrayList<>()).add(m);
            if (currentChatContact == null || !currentChatContact.equals(contact)) {
                int cnt = currentUser.unreadCounts.getOrDefault(contact, 0) + 1;
                currentUser.unreadCounts.put(contact, cnt);
                String snippet = m.getType().equals("FILE") ? "[File: " + m.getContent() + "]" : m.getContent();
                currentUser.unreadSnippets.put(contact,
                        snippet.length() > 20 ? snippet.substring(0, 20) + "..." : snippet);
            }
            saveData();
        }

        // Called by NetworkClient when a message is received.
        public void updateConversation(String contact, String messageStr) {
            if (currentChatContact != null && currentChatContact.equals(contact)) {
                conversationPanel.add(new JLabel(messageStr));
                conversationPanel.revalidate();
                conversationPanel.repaint();
            }
            Message m = new Message("", contact, currentUser.username, messageStr, "MSG", null);
            m.setStatus("READ");
            currentUser.chatHistory.computeIfAbsent(contact, k -> new ArrayList<>()).add(m);
            saveData();
        }
    }

    // NetworkClient: connects to ChatServer and handles sending/receiving messages.
    private class NetworkClient implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public NetworkClient(String username) {
            this.username = username;
            try {
                socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // Send username for identification.
                out.println(username);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(ChatClient.this, "Unable to connect to server: " + e.getMessage());
            }
        }

        // Send a message using protocol:
        // TYPE|messageId|sender|recipient|content|[optional fileData]
        public void sendMessage(String recipient, String message) {
            if (out != null) {
                out.println(message);
            }
        }

        public void run() {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    String[] parts = line.split("\\|", 6);
                    if (parts.length < 1)
                        continue;
                    String type = parts[0];
                    if (type.equals("MSG")) {
                        if (parts.length < 5)
                            continue;
                        String msgId = parts[1];
                        String sender = parts[2];
                        String recipient = parts[3];
                        String content = parts[4];
                        Message m = new Message(msgId, sender, recipient, content, "MSG", null);
                        m.setStatus("DELIVERED");
                        if (currentUser != null && !sender.equals(currentUser.username)) {
                            if (chatMainPanel.currentChatContact == null
                                    || !chatMainPanel.currentChatContact.equals(sender)) {
                                int cnt = currentUser.unreadCounts.getOrDefault(sender, 0) + 1;
                                currentUser.unreadCounts.put(sender, cnt);
                                String snippet = content.length() > 20 ? content.substring(0, 20) + "..." : content;
                                currentUser.unreadSnippets.put(sender, snippet);
                                chatMainPanel.refreshContacts();
                            }
                        }
                        currentUser.chatHistory.computeIfAbsent(sender, k -> new ArrayList<>()).add(m);
                        SwingUtilities.invokeLater(() -> {
                            chatMainPanel.updateConversation(sender, sender + ": " + content + " ✔");
                        });
                        if (chatMainPanel.currentChatContact != null
                                && chatMainPanel.currentChatContact.equals(sender)) {
                            sendMessage(sender, "ACK|" + msgId + "|READ");
                        }
                    } else if (type.equals("FILE")) {
                        if (parts.length < 6)
                            continue;
                        String msgId = parts[1];
                        String sender = parts[2];
                        String recipient = parts[3];
                        String filename = parts[4];
                        String base64data = parts[5];
                        Message m = new Message(msgId, sender, recipient, filename, "FILE", base64data);
                        m.setStatus("DELIVERED");
                        if (currentUser != null && !sender.equals(currentUser.username)) {
                            if (chatMainPanel.currentChatContact == null
                                    || !chatMainPanel.currentChatContact.equals(sender)) {
                                int cnt = currentUser.unreadCounts.getOrDefault(sender, 0) + 1;
                                currentUser.unreadCounts.put(sender, cnt);
                                currentUser.unreadSnippets.put(sender, "[File] " + filename);
                                chatMainPanel.refreshContacts();
                            }
                        }
                        currentUser.chatHistory.computeIfAbsent(sender, k -> new ArrayList<>()).add(m);
                        SwingUtilities.invokeLater(() -> {
                            chatMainPanel.updateConversation(sender, sender + " sent a file: " + filename + " ✔");
                        });
                        if (chatMainPanel.currentChatContact != null
                                && chatMainPanel.currentChatContact.equals(sender)) {
                            sendMessage(sender, "ACK|" + msgId + "|READ");
                        }
                    } else if (type.equals("ACK")) {
                        if (parts.length < 3)
                            continue;
                        String msgId = parts[1];
                        String status = parts[2];
                        SwingUtilities.invokeLater(() -> {
                            System.out.println("Message " + msgId + " status updated: " + status);
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void close() {
            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> {
            ChatClient client = new ChatClient();
            client.setVisible(true);
        });
    }
}
