import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.*;
import javax.swing.border.LineBorder;

/**
 * A modern dark-mode chat client with:
 * - Clear brand/title at the top
 * - More prominent buttons with improved text contrast
 * - Dark color scheme with an accent color
 * - Larger fonts for readability
 * - Single/double ticks for message status
 * - File sharing with a 'Download' button
 * - "Refresh Chat" button in the main chat panel
 * - A loader dialog that stays visible for 2 seconds
 */
public class ChatClient extends JFrame {

    // ----------- Color & Font Scheme -----------
    private static final Color DARK_BG = new Color(35, 35, 35); // Main background
    private static final Color DARKER_BG = new Color(25, 25, 25); // Panel background
    private static final Color LIGHT_TEXT = new Color(220, 220, 220);
    // Use a lighter accent background for buttons, so black text is visible:
    private static final Color ACCENT_COLOR = new Color(100, 255, 218);
    private static final Color BUTTON_TEXT = Color.BLACK;
    private static final Color FIELD_BG = new Color(60, 60, 60);

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 36);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 16);
    private static final Font FIELD_FONT = new Font("SansSerif", Font.PLAIN, 16);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 16);

    // Persistent storage file for users and chat history
    private static final String DATA_FILE = "chatapp_data.ser";
    // Map of registered users (username -> User)
    private Map<String, User> users;
    // Currently logged in user
    private User currentUser;

    // CardLayout for switching screens (login, register, chat)
    private CardLayout cardLayout;
    private JPanel mainPanel;

    // Network client for real-time messaging
    private NetworkClient networkClient;
    private final String SERVER_ADDRESS = "localhost";
    private final int SERVER_PORT = 12345;

    // Reference to ChatMainPanel to update conversation panels
    private ChatMainPanel chatMainPanel;

    // For generating unique message IDs
    private AtomicLong messageIdGenerator = new AtomicLong(System.currentTimeMillis());

    public ChatClient() {
        setTitle("Dark Mode Chat App - Chat Client");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        loadData();

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(DARK_BG);

        mainPanel.add(new LoginPanel(), "login");
        mainPanel.add(new RegistrationPanel(), "register");
        chatMainPanel = new ChatMainPanel();
        mainPanel.add(chatMainPanel, "chat");

        add(mainPanel);
        cardLayout.show(mainPanel, "login");
    }

    // Load persistent user data
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

    // Save persistent user data
    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving data: " + e.getMessage());
        }
    }

    // Quick loader method: show a dialog for 2 seconds
    private void showLoader(String message) {
        JDialog loader = new JDialog(this, "Loading", true);
        loader.setSize(300, 100);
        loader.setLocationRelativeTo(this);
        loader.setLayout(new BorderLayout());

        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setForeground(LIGHT_TEXT);
        label.setFont(LABEL_FONT);
        loader.getContentPane().setBackground(DARK_BG);
        loader.add(label, BorderLayout.CENTER);

        // Close after 2 seconds
        javax.swing.Timer t = new javax.swing.Timer(2000, e -> loader.dispose());

        t.setRepeats(false);
        t.start();

        loader.setVisible(true);
    }

    // ========== DATA MODELS ==========
    private static class User implements Serializable {
        private static final long serialVersionUID = 1L;
        String name;
        String username;
        String password;
        // Chat history: contact -> list of Message objects
        Map<String, List<Message>> chatHistory = new HashMap<>();
        // Unread counts: contact -> count of unread messages
        Map<String, Integer> unreadCounts = new HashMap<>();
        // Unread snippets: contact -> snippet text
        Map<String, String> unreadSnippets = new HashMap<>();

        User(String name, String username, String password) {
            this.name = name;
            this.username = username;
            this.password = password;
        }
    }

    // ========== LOGIN PANEL ==========
    private class LoginPanel extends JPanel {
        public LoginPanel() {
            setBackground(DARK_BG);
            setLayout(new BorderLayout());

            // Title
            JLabel title = new JLabel("Login", SwingConstants.CENTER);
            title.setFont(TITLE_FONT);
            title.setForeground(ACCENT_COLOR);
            add(title, BorderLayout.NORTH);

            // Form
            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBackground(DARK_BG);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            // Username label
            gbc.gridx = 0;
            gbc.gridy = 0;
            JLabel userLabel = new JLabel("Username:");
            userLabel.setFont(LABEL_FONT);
            userLabel.setForeground(LIGHT_TEXT);
            formPanel.add(userLabel, gbc);

            // Username field
            gbc.gridx = 1;
            JTextField usernameField = new JTextField(20);
            styleTextField(usernameField);
            formPanel.add(usernameField, gbc);

            // Password label
            gbc.gridx = 0;
            gbc.gridy = 1;
            JLabel passLabel = new JLabel("Password:");
            passLabel.setFont(LABEL_FONT);
            passLabel.setForeground(LIGHT_TEXT);
            formPanel.add(passLabel, gbc);

            // Password field
            gbc.gridx = 1;
            JPasswordField passwordField = new JPasswordField(20);
            styleTextField(passwordField);
            formPanel.add(passwordField, gbc);

            // Login button
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 2;
            JButton loginButton = new JButton("LOGIN");
            styleButton(loginButton);
            formPanel.add(loginButton, gbc);

            // Register button
            gbc.gridy = 3;
            JButton toRegisterButton = new JButton("REGISTER");
            styleButton(toRegisterButton);
            formPanel.add(toRegisterButton, gbc);

            add(formPanel, BorderLayout.CENTER);

            // Action Listeners
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
                        showLoader("Logging in...");
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

            toRegisterButton.addActionListener(e -> {
                showLoader("Opening Registration...");
                cardLayout.show(mainPanel, "register");
            });
        }
    }

    // ========== REGISTRATION PANEL ==========
    private class RegistrationPanel extends JPanel {
        public RegistrationPanel() {
            setBackground(DARK_BG);
            setLayout(new BorderLayout());

            JLabel title = new JLabel("Register", SwingConstants.CENTER);
            title.setFont(TITLE_FONT);
            title.setForeground(new Color(200, 100, 255));
            add(title, BorderLayout.NORTH);

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBackground(DARK_BG);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            // Name label
            gbc.gridx = 0;
            gbc.gridy = 0;
            JLabel nameLabel = new JLabel("Name:");
            nameLabel.setForeground(LIGHT_TEXT);
            nameLabel.setFont(LABEL_FONT);
            formPanel.add(nameLabel, gbc);

            gbc.gridx = 1;
            JTextField nameField = new JTextField(20);
            styleTextField(nameField);
            formPanel.add(nameField, gbc);

            // Username label
            gbc.gridx = 0;
            gbc.gridy = 1;
            JLabel userLabel = new JLabel("Username:");
            userLabel.setForeground(LIGHT_TEXT);
            userLabel.setFont(LABEL_FONT);
            formPanel.add(userLabel, gbc);

            gbc.gridx = 1;
            JTextField usernameField = new JTextField(20);
            styleTextField(usernameField);
            formPanel.add(usernameField, gbc);

            // Password label
            gbc.gridx = 0;
            gbc.gridy = 2;
            JLabel passLabel = new JLabel("Password:");
            passLabel.setForeground(LIGHT_TEXT);
            passLabel.setFont(LABEL_FONT);
            formPanel.add(passLabel, gbc);

            gbc.gridx = 1;
            JPasswordField passwordField = new JPasswordField(20);
            styleTextField(passwordField);
            formPanel.add(passwordField, gbc);

            // Register button
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.gridwidth = 2;
            JButton registerButton = new JButton("REGISTER");
            styleButton(registerButton);
            formPanel.add(registerButton, gbc);

            // Back to login
            gbc.gridy = 4;
            JButton backToLoginButton = new JButton("BACK TO LOGIN");
            styleButton(backToLoginButton);
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
                showLoader("Registering...");
                JOptionPane.showMessageDialog(this, "Registration successful! Please login.", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                cardLayout.show(mainPanel, "login");
            });

            backToLoginButton.addActionListener(e -> {
                showLoader("Going back to Login...");
                cardLayout.show(mainPanel, "login");
            });
        }
    }

    // ========== MAIN CHAT PANEL ==========
    private class ChatMainPanel extends JPanel {
        private DefaultListModel<String> contactsModel;
        private JList<String> contactsList;
        private JPanel chatSessionPanel;
        private JLabel headerLabel;
        private String currentChatContact = null;
        private JPanel conversationPanel;

        public ChatMainPanel() {
            setLayout(new BorderLayout());
            setBackground(DARK_BG);

            // Header bar
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(DARKER_BG);
            headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            headerLabel = new JLabel("Welcome, ...", SwingConstants.LEFT);
            headerLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
            headerLabel.setForeground(ACCENT_COLOR);
            headerPanel.add(headerLabel, BorderLayout.WEST);

            // RIGHT side: Refresh + Logout
            JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            rightPanel.setOpaque(false);

            JButton refreshButton = new JButton("REFRESH CHAT");
            styleButton(refreshButton);
            refreshButton.addActionListener(e -> {
                loadData();
                refreshContacts();
                refreshChatHistory();
                JOptionPane.showMessageDialog(this, "Chats Refreshed!");
            });

            JButton logoutButton = new JButton("LOGOUT");
            styleButton(logoutButton);
            logoutButton.addActionListener(e -> {
                if (networkClient != null)
                    networkClient.close();
                currentUser = null;
                cardLayout.show(mainPanel, "login");
            });

            rightPanel.add(refreshButton);
            rightPanel.add(logoutButton);

            headerPanel.add(rightPanel, BorderLayout.EAST);
            add(headerPanel, BorderLayout.NORTH);

            // Contacts list
            contactsModel = new DefaultListModel<>();
            contactsList = new JList<>(contactsModel);
            contactsList.setBackground(DARKER_BG);
            contactsList.setForeground(LIGHT_TEXT);
            contactsList.setFont(FIELD_FONT);

            JScrollPane contactsScroll = new JScrollPane(contactsList);
            contactsScroll.setPreferredSize(new Dimension(250, 0));
            contactsScroll.setBorder(BorderFactory.createTitledBorder(
                    new LineBorder(ACCENT_COLOR), "Contacts"));
            contactsScroll.getViewport().setBackground(DARKER_BG);

            // Chat session
            chatSessionPanel = new JPanel(new BorderLayout());
            chatSessionPanel.setBackground(DARKER_BG);
            chatSessionPanel.setBorder(BorderFactory.createTitledBorder(
                    new LineBorder(ACCENT_COLOR), "Conversation"));

            contactsList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        String display = contactsList.getSelectedValue();
                        if (display != null) {
                            String contact = display.split(" ")[0];
                            openChatSession(contact);
                        }
                    }
                }
            });

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, contactsScroll, chatSessionPanel);
            splitPane.setDividerLocation(250);
            add(splitPane, BorderLayout.CENTER);
        }

        public void refreshContacts() {
            if (currentUser == null)
                return;
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
            // Reload from currentUser (already in memory).
            // If you want to force from disk, call loadData() first.
        }

        public void openChatSession(String contact) {
            currentChatContact = contact;
            currentUser.unreadCounts.put(contact, 0);
            currentUser.unreadSnippets.remove(contact);
            refreshContacts();

            chatSessionPanel.removeAll();
            chatSessionPanel.setLayout(new BorderLayout());

            conversationPanel = new JPanel();
            conversationPanel.setLayout(new BoxLayout(conversationPanel, BoxLayout.Y_AXIS));
            conversationPanel.setBackground(DARK_BG);

            JScrollPane convScroll = new JScrollPane(conversationPanel);
            convScroll.setBorder(null);
            convScroll.getViewport().setBackground(DARK_BG);

            // Load conversation
            List<Message> history = currentUser.chatHistory.get(contact);
            if (history != null) {
                for (Message m : history) {
                    conversationPanel.add(createMessagePanel(m));
                }
            }

            // Input panel
            JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
            inputPanel.setBackground(DARKER_BG);

            JTextField inputField = new JTextField(30);
            styleTextField(inputField);

            JButton sendButton = new JButton("Send");
            styleButton(sendButton);

            JButton sendFileButton = new JButton("Send File");
            styleButton(sendFileButton);

            JButton closeChatButton = new JButton("Close Chat");
            styleButton(closeChatButton);

            sendButton.addActionListener(e -> {
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
                JFileChooser fc = new JFileChooser();
                int res = fc.showOpenDialog(chatSessionPanel);
                if (res == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    try {
                        byte[] data = Files.readAllBytes(file.toPath());
                        String base64Encoded = Base64.getEncoder().encodeToString(data);
                        String msgId = currentUser.username + "-" + messageIdGenerator.getAndIncrement();
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
                        JOptionPane.showMessageDialog(chatSessionPanel, "Error reading file: " + ex.getMessage());
                    }
                }
            });

            closeChatButton.addActionListener(e -> {
                chatSessionPanel.removeAll();
                chatSessionPanel.revalidate();
                chatSessionPanel.repaint();
                currentChatContact = null;
            });

            inputPanel.add(inputField);
            inputPanel.add(sendButton);
            inputPanel.add(sendFileButton);
            inputPanel.add(closeChatButton);

            chatSessionPanel.add(convScroll, BorderLayout.CENTER);
            chatSessionPanel.add(inputPanel, BorderLayout.SOUTH);
            chatSessionPanel.revalidate();
            chatSessionPanel.repaint();

            // Mark messages as read
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

        private JPanel createMessagePanel(Message m) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            panel.setBackground(DARKER_BG);

            JLabel msgLabel;
            if (m.getType().equals("MSG")) {
                msgLabel = new JLabel(m.getSender() + ": " + m.getContent());
            } else if (m.getType().equals("FILE")) {
                msgLabel = new JLabel(m.getSender() + " sent a file: " + m.getContent());
            } else {
                msgLabel = new JLabel(m.getSender() + ": " + m.getContent());
            }
            msgLabel.setForeground(LIGHT_TEXT);
            panel.add(msgLabel, BorderLayout.CENTER);

            JLabel statusLabel = new JLabel();
            statusLabel.setForeground(new Color(0, 255, 0)); // bright green ticks
            if (m.getSender().equals(currentUser.username)) {
                switch (m.getStatus()) {
                    case "DELIVERED":
                        statusLabel.setText(" ✔");
                        break;
                    case "READ":
                        statusLabel.setText(" ✔✔");
                        break;
                    default:
                        statusLabel.setText(" …");
                        break;
                }
            }
            panel.add(statusLabel, BorderLayout.EAST);

            if (m.getType().equals("FILE")) {
                JButton downloadBtn = new JButton("Download");
                styleButton(downloadBtn);
                downloadBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
                downloadBtn.addActionListener(e -> {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setSelectedFile(new File(m.getContent()));
                    int res = chooser.showSaveDialog(chatSessionPanel);
                    if (res == JFileChooser.APPROVE_OPTION) {
                        File outFile = chooser.getSelectedFile();
                        try {
                            byte[] data = Base64.getDecoder().decode(m.getFileData());
                            Files.write(outFile.toPath(), data);
                            JOptionPane.showMessageDialog(chatSessionPanel,
                                    "File downloaded to " + outFile.getAbsolutePath());
                            downloadBtn.setEnabled(false);
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(chatSessionPanel, "Error saving file: " + ex.getMessage());
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
                if (snippet.length() > 20)
                    snippet = snippet.substring(0, 20) + "...";
                currentUser.unreadSnippets.put(contact, snippet);
            }
            saveData();
        }

        public void updateConversation(String contact, String displayText) {
            if (currentChatContact != null && currentChatContact.equals(contact)) {
                JLabel label = new JLabel(displayText);
                label.setForeground(LIGHT_TEXT);
                JPanel p = new JPanel(new BorderLayout());
                p.setBackground(DARKER_BG);
                p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                p.add(label, BorderLayout.CENTER);
                conversationPanel.add(p);
                conversationPanel.revalidate();
                conversationPanel.repaint();
            }
        }
    }

    // ========== NETWORK CLIENT ==========
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
                out.println(username); // identify
            } catch (IOException e) {
                JOptionPane.showMessageDialog(ChatClient.this, "Unable to connect to server: " + e.getMessage());
            }
        }

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
                            m.setStatus("READ");
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
                            m.setStatus("READ");
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

    // Style text fields
    private void styleTextField(JTextField field) {
        field.setFont(FIELD_FONT);
        field.setBackground(FIELD_BG);
        field.setForeground(LIGHT_TEXT);
        field.setCaretColor(LIGHT_TEXT);
        field.setBorder(new LineBorder(ACCENT_COLOR, 1));
    }

    // Style buttons: accent background, black text
    private void styleButton(JButton button) {
        button.setBackground(ACCENT_COLOR);
        button.setForeground(BUTTON_TEXT);
        button.setFont(BUTTON_FONT);
        button.setFocusPainted(false);
        button.setBorder(new LineBorder(Color.WHITE, 1));
    }

    public static void main(String[] args) {
        // Optionally set system look & feel
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
