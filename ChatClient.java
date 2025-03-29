import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;

public class ChatClient extends JFrame {

    // ------------- Color and Font Constants -------------
    private static final Color DARK_BG       = new Color(35, 35, 35);
    private static final Color DARKER_BG     = new Color(25, 25, 25);
    private static final Color LIGHT_TEXT    = new Color(220, 220, 220);
    private static final Color ACCENT_COLOR  = new Color(100, 255, 218);
    private static final Color BUTTON_BG     = new Color(100, 255, 218);
    private static final Color BUTTON_TEXT   = Color.BLACK;
    private static final Color FIELD_BG      = new Color(60, 60, 60);

    private static final Font TITLE_FONT  = new Font("SansSerif", Font.BOLD, 36);
    private static final Font LABEL_FONT  = new Font("SansSerif", Font.BOLD, 16);
    private static final Font FIELD_FONT  = new Font("SansSerif", Font.PLAIN, 16);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 16);

    // ------------- Database Info -------------
    private static final String DB_URL = "jdbc:sqlite:chatapp.db";

    // ------------- Data Models -------------
    public static class User implements Serializable {
        private static final long serialVersionUID = 1L;
        public String name;
        public String username;
        public String password;
        public String profilePhotoBase64;
        public Map<String, List<Message>> chatHistory = new HashMap<>();
        public Map<String, Integer> unreadCounts = new HashMap<>();
        public Map<String, String> unreadSnippets = new HashMap<>();

        public User(String name, String username, String password) {
            this.name = name;
            this.username = username;
            this.password = password;
            this.profilePhotoBase64 = null;
        }
    }

    public static class Message implements Serializable {
        private static final long serialVersionUID = 1L;
        private String messageId;
        private String sender;
        private String recipient;
        private String content;
        private String type;      // "MSG", "FILE", "GROUP_MSG", or "GROUP_FILE"
        private String fileData;
        private String status;
        private long timestamp;

        public Message(String messageId, String sender, String recipient, String content, String type, String fileData) {
            this.messageId = messageId;
            this.sender = sender;
            this.recipient = recipient;
            this.content = content;
            this.type = type;
            this.fileData = fileData;
            this.status = "PENDING";
            this.timestamp = System.currentTimeMillis();
        }
        public String getMessageId() { return messageId; }
        public String getSender() { return sender; }
        public String getRecipient() { return recipient; }
        public String getContent() { return content; }
        public String getType() { return type; }
        public String getFileData() { return fileData; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    // ------------- UI Components -------------
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private ChatMainPanel chatMainPanel;
    private ProfilePanel profilePanel;

    // ------------- Network Components -------------
    private NetworkClient networkClient;
    private final String SERVER_ADDRESS = "localhost";
    private final int SERVER_PORT = 12345;

    // ------------- Others -------------
    private AtomicLong messageIdGenerator = new AtomicLong(System.currentTimeMillis());
    private SQLDatabase db;
    private Map<String, User> users;
    private User currentUser;
    // Local mapping for groups: groupName -> Set of members
    private Map<String, Set<String>> groups = new HashMap<>();

    // ------------- Constructor -------------
    public ChatClient() {
        setTitle("Dark Mode Chat App - Chat Client");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        db = new SQLDatabase();
        db.initialize();
        users = db.loadUsers();

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(DARK_BG);

        mainPanel.add(new LoginPanel(), "login");
        mainPanel.add(new RegistrationPanel(), "register");
        chatMainPanel = new ChatMainPanel();
        mainPanel.add(chatMainPanel, "chat");
        profilePanel = new ProfilePanel();
        mainPanel.add(profilePanel, "profile");

        add(mainPanel);
        cardLayout.show(mainPanel, "login");
    }

    // ------------- Utility Methods -------------
    private void showLoader(String message) {
        // Retained for profile updates and registration; auto-refresh does not show loader.
        JDialog loader = new JDialog(this, "Loading", true);
        loader.setSize(300, 100);
        loader.setLocationRelativeTo(this);
        loader.setLayout(new BorderLayout());
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setForeground(LIGHT_TEXT);
        label.setFont(LABEL_FONT);
        loader.getContentPane().setBackground(DARK_BG);
        loader.add(label, BorderLayout.CENTER);
        javax.swing.Timer t = new javax.swing.Timer(2000, e -> loader.dispose());
        t.setRepeats(false);
        t.start();
        loader.setVisible(true);
    }

    private void styleTextField(JTextField field) {
        field.setFont(FIELD_FONT);
        field.setBackground(FIELD_BG);
        field.setForeground(LIGHT_TEXT);
        field.setCaretColor(LIGHT_TEXT);
        field.setBorder(new LineBorder(ACCENT_COLOR, 1));
    }

    private void styleButton(JButton button) {
        button.setBackground(BUTTON_BG);
        button.setForeground(BUTTON_TEXT);
        button.setFont(BUTTON_FONT);
        button.setFocusPainted(false);
        button.setBorder(new LineBorder(Color.WHITE, 1));
    }

    // ------------- LOGIN PANEL -------------
    private class LoginPanel extends JPanel {
        public LoginPanel() {
            setBackground(DARK_BG);
            setLayout(new BorderLayout());

            JLabel title = new JLabel("Login", SwingConstants.CENTER);
            title.setFont(TITLE_FONT);
            title.setForeground(ACCENT_COLOR);
            add(title, BorderLayout.NORTH);

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBackground(DARK_BG);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0; gbc.gridy = 0;
            JLabel userLabel = new JLabel("Username:");
            userLabel.setFont(LABEL_FONT);
            userLabel.setForeground(LIGHT_TEXT);
            formPanel.add(userLabel, gbc);

            gbc.gridx = 1;
            JTextField usernameField = new JTextField(20);
            styleTextField(usernameField);
            formPanel.add(usernameField, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            JLabel passLabel = new JLabel("Password:");
            passLabel.setFont(LABEL_FONT);
            passLabel.setForeground(LIGHT_TEXT);
            formPanel.add(passLabel, gbc);

            gbc.gridx = 1;
            JPasswordField passwordField = new JPasswordField(20);
            styleTextField(passwordField);
            formPanel.add(passwordField, gbc);

            gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
            JButton loginButton = new JButton("LOGIN");
            styleButton(loginButton);
            formPanel.add(loginButton, gbc);

            gbc.gridy = 3;
            JButton toRegisterButton = new JButton("REGISTER");
            styleButton(toRegisterButton);
            formPanel.add(toRegisterButton, gbc);

            add(formPanel, BorderLayout.CENTER);

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
                        currentUser.chatHistory = db.loadMessagesForUser(currentUser.username);
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

    // ------------- REGISTRATION PANEL -------------
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

            gbc.gridx = 0; gbc.gridy = 0;
            JLabel nameLabel = new JLabel("Name:");
            nameLabel.setFont(LABEL_FONT);
            nameLabel.setForeground(LIGHT_TEXT);
            formPanel.add(nameLabel, gbc);

            gbc.gridx = 1;
            JTextField nameField = new JTextField(20);
            styleTextField(nameField);
            formPanel.add(nameField, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            JLabel userLabel = new JLabel("Username:");
            userLabel.setFont(LABEL_FONT);
            userLabel.setForeground(LIGHT_TEXT);
            formPanel.add(userLabel, gbc);

            gbc.gridx = 1;
            JTextField usernameField = new JTextField(20);
            styleTextField(usernameField);
            formPanel.add(usernameField, gbc);

            gbc.gridx = 0; gbc.gridy = 2;
            JLabel passLabel = new JLabel("Password:");
            passLabel.setFont(LABEL_FONT);
            passLabel.setForeground(LIGHT_TEXT);
            formPanel.add(passLabel, gbc);

            gbc.gridx = 1;
            JPasswordField passwordField = new JPasswordField(20);
            styleTextField(passwordField);
            formPanel.add(passwordField, gbc);

            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
            JButton registerButton = new JButton("REGISTER");
            styleButton(registerButton);
            formPanel.add(registerButton, gbc);

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
                db.saveUser(newUser);
                showLoader("Registering...");
                JOptionPane.showMessageDialog(this, "Registration successful! Please login.", "Success", JOptionPane.INFORMATION_MESSAGE);
                cardLayout.show(mainPanel, "login");
            });

            backToLoginButton.addActionListener(e -> {
                showLoader("Going back to Login...");
                cardLayout.show(mainPanel, "login");
            });
        }
    }

    // ------------- PROFILE PANEL -------------
    private class ProfilePanel extends JPanel {
        private JLabel photoLabel;
        private JTextField nameField, usernameField;
        private JPasswordField passwordField;
        private JButton saveButton, loadPhotoButton;
        public ProfilePanel() {
            setLayout(new BorderLayout());
            setBackground(DARK_BG);

            JLabel title = new JLabel("Profile Settings", SwingConstants.CENTER);
            title.setFont(TITLE_FONT);
            title.setForeground(new Color(255, 200, 100));
            add(title, BorderLayout.NORTH);

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBackground(DARK_BG);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10,10,10,10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0; gbc.gridy = 0;
            JLabel nameLabel = new JLabel("Name:");
            nameLabel.setFont(LABEL_FONT);
            nameLabel.setForeground(LIGHT_TEXT);
            formPanel.add(nameLabel, gbc);
            gbc.gridx = 1;
            nameField = new JTextField(20);
            styleTextField(nameField);
            formPanel.add(nameField, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            JLabel userLabel = new JLabel("Username:");
            userLabel.setFont(LABEL_FONT);
            userLabel.setForeground(LIGHT_TEXT);
            formPanel.add(userLabel, gbc);
            gbc.gridx = 1;
            usernameField = new JTextField(20);
            styleTextField(usernameField);
            formPanel.add(usernameField, gbc);

            gbc.gridx = 0; gbc.gridy = 2;
            JLabel passLabel = new JLabel("Password:");
            passLabel.setFont(LABEL_FONT);
            passLabel.setForeground(LIGHT_TEXT);
            formPanel.add(passLabel, gbc);
            gbc.gridx = 1;
            passwordField = new JPasswordField(20);
            styleTextField(passwordField);
            formPanel.add(passwordField, gbc);

            gbc.gridx = 0; gbc.gridy = 3;
            JLabel photoTextLabel = new JLabel("Profile Photo:");
            photoTextLabel.setFont(LABEL_FONT);
            photoTextLabel.setForeground(LIGHT_TEXT);
            formPanel.add(photoTextLabel, gbc);
            gbc.gridx = 1;
            photoLabel = new JLabel();
            photoLabel.setPreferredSize(new Dimension(100, 100));
            photoLabel.setOpaque(true);
            photoLabel.setBackground(FIELD_BG);
            formPanel.add(photoLabel, gbc);

            gbc.gridx = 1; gbc.gridy = 4;
            loadPhotoButton = new JButton("Load Photo");
            styleButton(loadPhotoButton);
            formPanel.add(loadPhotoButton, gbc);

            gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
            saveButton = new JButton("SAVE CHANGES");
            styleButton(saveButton);
            formPanel.add(saveButton, gbc);

            add(formPanel, BorderLayout.CENTER);

            loadPhotoButton.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                int res = fc.showOpenDialog(ProfilePanel.this);
                if (res == JFileChooser.APPROVE_OPTION) {
                    File f = fc.getSelectedFile();
                    try {
                        byte[] data = Files.readAllBytes(f.toPath());
                        String base64 = Base64.getEncoder().encodeToString(data);
                        currentUser.profilePhotoBase64 = base64;
                        ImageIcon icon = new ImageIcon(Base64.getDecoder().decode(base64));
                        Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                        photoLabel.setIcon(new ImageIcon(img));
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(ProfilePanel.this, "Error loading photo: " + ex.getMessage());
                    }
                }
            });

            saveButton.addActionListener(e -> {
                if (currentUser == null)
                    return;
                showLoader("Saving Profile...");
                currentUser.name = nameField.getText().trim();
                currentUser.username = usernameField.getText().trim();
                currentUser.password = new String(passwordField.getPassword());
                db.updateUser(currentUser);
                JOptionPane.showMessageDialog(ProfilePanel.this, "Profile updated!");
                chatMainPanel.refreshContacts();
                cardLayout.show(mainPanel, "chat");
            });
        }

        public void loadProfileData() {
            if (currentUser != null) {
                nameField.setText(currentUser.name);
                usernameField.setText(currentUser.username);
                passwordField.setText(currentUser.password);
                if (currentUser.profilePhotoBase64 != null) {
                    ImageIcon icon = new ImageIcon(Base64.getDecoder().decode(currentUser.profilePhotoBase64));
                    Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                    photoLabel.setIcon(new ImageIcon(img));
                } else {
                    photoLabel.setIcon(null);
                    photoLabel.setText("No Photo");
                }
            }
        }
    }

    // ------------- MAIN CHAT PANEL -------------
    private class ChatMainPanel extends JPanel {
        private DefaultListModel<String> contactsModel;
        private JList<String> contactsList;
        private JPanel chatSessionPanel;
        private JLabel headerLabel;
        // currentChatContact stores either an individual username or "Group: groupName"
        private String currentChatContact = null;
        private JPanel conversationPanel;

        public ChatMainPanel() {
            setLayout(new BorderLayout());
            setBackground(DARK_BG);

            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(DARKER_BG);
            headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            headerLabel = new JLabel("Welcome, ...", SwingConstants.LEFT);
            headerLabel.setFont(TITLE_FONT);
            headerLabel.setForeground(ACCENT_COLOR);
            headerPanel.add(headerLabel, BorderLayout.WEST);

            JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            rightPanel.setOpaque(false);
            JButton refreshChatButton = new JButton("REFRESH CHAT");
            styleButton(refreshChatButton);
            refreshChatButton.addActionListener(e -> {
                users = db.loadUsers();
                refreshContacts();
                refreshChatHistory();
                JOptionPane.showMessageDialog(ChatMainPanel.this, "Chats Refreshed!");
            });
            JButton profileButton = new JButton("PROFILE");
            styleButton(profileButton);
            profileButton.addActionListener(e -> {
                if (currentUser != null) {
                    profilePanel.loadProfileData();
                    cardLayout.show(mainPanel, "profile");
                }
            });
            JButton logoutButton = new JButton("LOGOUT");
            styleButton(logoutButton);
            logoutButton.addActionListener(e -> {
                if (networkClient != null)
                    networkClient.close();
                currentUser = null;
                cardLayout.show(mainPanel, "login");
            });
            rightPanel.add(refreshChatButton);
            rightPanel.add(profileButton);
            rightPanel.add(logoutButton);
            headerPanel.add(rightPanel, BorderLayout.EAST);
            add(headerPanel, BorderLayout.NORTH);

            contactsModel = new DefaultListModel<>();
            contactsList = new JList<>(contactsModel);
            contactsList.setBackground(DARKER_BG);
            contactsList.setForeground(LIGHT_TEXT);
            contactsList.setFont(FIELD_FONT);
            JScrollPane contactsScroll = new JScrollPane(contactsList);
            contactsScroll.setPreferredSize(new Dimension(250, 0));
            contactsScroll.setBorder(BorderFactory.createTitledBorder(new LineBorder(ACCENT_COLOR), "Contacts"));
            contactsScroll.getViewport().setBackground(DARKER_BG);

            JButton createGroupButton = new JButton("Create Group");
            styleButton(createGroupButton);
            createGroupButton.addActionListener(e -> openCreateGroupDialog());

            JPanel leftPanel = new JPanel(new BorderLayout());
            leftPanel.add(createGroupButton, BorderLayout.NORTH);
            leftPanel.add(contactsScroll, BorderLayout.CENTER);

            chatSessionPanel = new JPanel(new BorderLayout());
            chatSessionPanel.setBackground(DARKER_BG);
            chatSessionPanel.setBorder(BorderFactory.createTitledBorder(new LineBorder(ACCENT_COLOR), "Conversation"));

            contactsList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        String display = contactsList.getSelectedValue();
                        if (display != null) {
                            if (display.startsWith("Group:")) {
                                String groupName = display.substring(6).trim();
                                openGroupChatSession(groupName);
                            } else {
                                String contact = display.split(" ")[0];
                                openIndividualChatSession(contact);
                            }
                        }
                    }
                }
            });

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, chatSessionPanel);
            splitPane.setDividerLocation(250);
            add(splitPane, BorderLayout.CENTER);

            // Auto-refresh chats every 5 seconds without loading screens
            new javax.swing.Timer(5000, e -> {
                users = db.loadUsers();
                refreshContacts();
                refreshChatHistory();
            }).start();
        }

        public void refreshContacts() {
            if (currentUser == null)
                return;
            headerLabel.setText("Welcome, " + currentUser.name);
            contactsModel.clear();
            // Add individual contacts
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
            // Add groups from local mapping
            for (String groupName : groups.keySet()) {
                contactsModel.addElement("Group: " + groupName);
            }
        }

        public void refreshChatHistory() {
            // Optionally, update conversation panels if needed.
        }

        public void openIndividualChatSession(String contact) {
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

            List<Message> history = currentUser.chatHistory.get(contact);
            if (history != null) {
                for (Message m : history) {
                    conversationPanel.add(createMessagePanel(m));
                }
            }

            JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
            inputPanel.setBackground(DARKER_BG);
            JTextField inputField = new JTextField(30);
            styleTextField(inputField);
            JButton sendMsgButton = new JButton("Send");
            styleButton(sendMsgButton);
            JButton sendFileButton = new JButton("Send File");
            styleButton(sendFileButton);
            JButton closeChatButton = new JButton("Close Chat");
            styleButton(closeChatButton);

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
                        networkClient.sendMessage(contact, "MSG|" + msgId + "|" + currentUser.username + "|" + contact + "|" + msgText);
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
                        Message fileMsg = new Message(msgId, currentUser.username, contact, file.getName(), "FILE", base64Encoded);
                        fileMsg.setStatus("PENDING");
                        addMessageToHistory(contact, fileMsg);
                        conversationPanel.add(createMessagePanel(fileMsg));
                        conversationPanel.revalidate();
                        conversationPanel.repaint();
                        if (networkClient != null) {
                            networkClient.sendMessage(contact, "FILE|" + msgId + "|" + currentUser.username + "|" + contact + "|" + file.getName() + "|" + base64Encoded);
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
            inputPanel.add(sendMsgButton);
            inputPanel.add(sendFileButton);
            inputPanel.add(closeChatButton);

            chatSessionPanel.add(convScroll, BorderLayout.CENTER);
            chatSessionPanel.add(inputPanel, BorderLayout.SOUTH);
            chatSessionPanel.revalidate();
            chatSessionPanel.repaint();

            if (currentUser.chatHistory.containsKey(contact)) {
                for (Message m : currentUser.chatHistory.get(contact)) {
                    if (!"READ".equals(m.getStatus()) && m.getSender().equals(contact)) {
                        m.setStatus("READ");
                        if (networkClient != null) {
                            networkClient.sendMessage(contact, "ACK|" + m.getMessageId() + "|READ");
                        }
                    }
                }
                db.saveMessagesForUser(currentUser.username, currentUser.chatHistory);
            }
        }

        public void openGroupChatSession(String groupName) {
            currentChatContact = "Group: " + groupName;
            chatSessionPanel.removeAll();
            chatSessionPanel.setLayout(new BorderLayout());

            // Group header with popup menu for options
            JPanel groupHeader = new JPanel(new BorderLayout());
            groupHeader.setBackground(DARKER_BG);
            JLabel groupLabel = new JLabel("Group: " + groupName);
            groupLabel.setFont(TITLE_FONT);
            groupLabel.setForeground(ACCENT_COLOR);
            groupHeader.add(groupLabel, BorderLayout.WEST);

            groupLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem leaveItem = new JMenuItem("Leave Group");
                    JMenuItem changeNameItem = new JMenuItem("Change Group Name");
                    JMenuItem showMembersItem = new JMenuItem("Show Members");
                    JMenuItem addUserItem = new JMenuItem("Add User");

                    leaveItem.addActionListener(ae -> {
                        if (networkClient != null) {
                            networkClient.sendMessage("", "LEAVE_GROUP|" + groupName + "|" + currentUser.username);
                            groups.remove(groupName);
                            JOptionPane.showMessageDialog(ChatMainPanel.this, "You have left the group.");
                            chatSessionPanel.removeAll();
                            chatSessionPanel.revalidate();
                            chatSessionPanel.repaint();
                            currentChatContact = null;
                            refreshContacts();
                        }
                    });

                    changeNameItem.addActionListener(ae -> {
                        String newName = JOptionPane.showInputDialog(ChatMainPanel.this, "Enter new group name:");
                        if (newName != null && !newName.trim().isEmpty() && networkClient != null) {
                            networkClient.sendMessage("", "UPDATE_GROUP|" + groupName + "|" + newName + "|" + currentUser.username);
                            Set<String> members = groups.get(groupName);
                            groups.remove(groupName);
                            groups.put(newName, members);
                            groupLabel.setText("Group: " + newName);
                            refreshContacts();
                        }
                    });

                    showMembersItem.addActionListener(ae -> {
                        if (networkClient != null) {
                            networkClient.sendMessage("", "GROUP_INFO|" + groupName);
                        }
                        SwingUtilities.invokeLater(() -> {
                            Set<String> mem = groups.get(groupName);
                            if (mem != null) {
                                JOptionPane.showMessageDialog(ChatMainPanel.this, "Members: " + String.join(", ", mem));
                            } else {
                                JOptionPane.showMessageDialog(ChatMainPanel.this, "No member info available.");
                            }
                        });
                    });

                    // "Add User" now shows checkboxes for remaining available users
                    addUserItem.addActionListener(ae -> {
                        Set<String> currentMembers = groups.get(groupName);
                        List<String> availableUsers = new ArrayList<>();
                        for(String uname : users.keySet()){
                            if(!uname.equals(currentUser.username) && (currentMembers == null || !currentMembers.contains(uname))){
                                availableUsers.add(uname);
                            }
                        }
                        if(availableUsers.isEmpty()){
                            JOptionPane.showMessageDialog(ChatMainPanel.this, "No users available to add.");
                            return;
                        }
                        JPanel panel = new JPanel();
                        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                        Map<String, JCheckBox> checkboxMap = new HashMap<>();
                        for(String user: availableUsers){
                            JCheckBox cb = new JCheckBox(user);
                            cb.setForeground(LIGHT_TEXT);
                            cb.setBackground(DARK_BG);
                            panel.add(cb);
                            checkboxMap.put(user, cb);
                        }
                        int result = JOptionPane.showConfirmDialog(ChatMainPanel.this, panel, "Select users to add", JOptionPane.OK_CANCEL_OPTION);
                        if(result == JOptionPane.OK_OPTION){
                            for(Map.Entry<String, JCheckBox> entry: checkboxMap.entrySet()){
                                if(entry.getValue().isSelected()){
                                    networkClient.sendMessage("", "ADD_TO_GROUP|" + groupName + "|" + currentUser.username + "|" + entry.getKey());
                                }
                            }
                        }
                    });

                    menu.add(leaveItem);
                    menu.add(changeNameItem);
                    menu.add(showMembersItem);
                    menu.add(addUserItem);
                    menu.show(groupLabel, e.getX(), e.getY());
                }
            });

            chatSessionPanel.add(groupHeader, BorderLayout.NORTH);

            conversationPanel = new JPanel();
            conversationPanel.setLayout(new BoxLayout(conversationPanel, BoxLayout.Y_AXIS));
            conversationPanel.setBackground(DARK_BG);
            JScrollPane convScroll = new JScrollPane(conversationPanel);
            convScroll.setBorder(null);
            convScroll.getViewport().setBackground(DARK_BG);

            List<Message> history = currentUser.chatHistory.get("Group:" + groupName);
            if (history != null) {
                for (Message m : history) {
                    conversationPanel.add(createMessagePanel(m));
                }
            }

            JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
            inputPanel.setBackground(DARKER_BG);
            JTextField inputField = new JTextField(30);
            styleTextField(inputField);
            JButton sendMsgButton = new JButton("Send");
            styleButton(sendMsgButton);
            JButton sendFileButton = new JButton("Send File");
            styleButton(sendFileButton);
            JButton closeChatButton = new JButton("Close Chat");
            styleButton(closeChatButton);

            sendMsgButton.addActionListener(e -> {
                String msgText = inputField.getText().trim();
                if (!msgText.isEmpty()) {
                    String msgId = currentUser.username + "-" + messageIdGenerator.getAndIncrement();
                    Message msg = new Message(msgId, currentUser.username, groupName, msgText, "GROUP_MSG", null);
                    msg.setStatus("PENDING");
                    addMessageToHistory("Group:" + groupName, msg);
                    conversationPanel.add(createMessagePanel(msg));
                    conversationPanel.revalidate();
                    conversationPanel.repaint();
                    inputField.setText("");
                    if (networkClient != null) {
                        networkClient.sendMessage("", "GROUP_MSG|" + msgId + "|" + currentUser.username + "|" + groupName + "|" + msgText);
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
                        Message fileMsg = new Message(msgId, currentUser.username, groupName, file.getName(), "GROUP_FILE", base64Encoded);
                        fileMsg.setStatus("PENDING");
                        addMessageToHistory("Group:" + groupName, fileMsg);
                        conversationPanel.add(createMessagePanel(fileMsg));
                        conversationPanel.revalidate();
                        conversationPanel.repaint();
                        if (networkClient != null) {
                            networkClient.sendMessage("", "GROUP_FILE|" + msgId + "|" + currentUser.username + "|" + groupName + "|" + file.getName() + "|" + base64Encoded);
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
            inputPanel.add(sendMsgButton);
            inputPanel.add(sendFileButton);
            inputPanel.add(closeChatButton);

            chatSessionPanel.add(convScroll, BorderLayout.CENTER);
            chatSessionPanel.add(inputPanel, BorderLayout.SOUTH);
            chatSessionPanel.revalidate();
            chatSessionPanel.repaint();
        }

        private void openCreateGroupDialog() {
            JDialog groupDialog = new JDialog(ChatClient.this, "Create Group", true);
            groupDialog.setSize(400, 400);
            groupDialog.setLocationRelativeTo(ChatClient.this);
            groupDialog.setLayout(new BorderLayout());

            JPanel selectionPanel = new JPanel();
            selectionPanel.setLayout(new BoxLayout(selectionPanel, BoxLayout.Y_AXIS));
            Map<String, JCheckBox> checkBoxes = new HashMap<>();
            for (String uname : users.keySet()) {
                if (!uname.equals(currentUser.username)) {
                    JCheckBox cb = new JCheckBox(uname);
                    cb.setForeground(LIGHT_TEXT);
                    cb.setBackground(DARK_BG);
                    checkBoxes.put(uname, cb);
                    selectionPanel.add(cb);
                }
            }
            JScrollPane scrollPane = new JScrollPane(selectionPanel);
            scrollPane.setBorder(BorderFactory.createTitledBorder("Select Contacts"));
            scrollPane.getViewport().setBackground(DARK_BG);

            JPanel groupNamePanel = new JPanel(new FlowLayout());
            groupNamePanel.setBackground(DARK_BG);
            JLabel nameLabel = new JLabel("Group Name:");
            nameLabel.setForeground(LIGHT_TEXT);
            JTextField groupNameField = new JTextField(20);
            styleTextField(groupNameField);
            groupNamePanel.add(nameLabel);
            groupNamePanel.add(groupNameField);

            JButton createBtn = new JButton("Create");
            styleButton(createBtn);
            createBtn.addActionListener(e -> {
                String groupName = groupNameField.getText().trim();
                if (groupName.isEmpty()) {
                    JOptionPane.showMessageDialog(groupDialog, "Please enter a group name.");
                    return;
                }
                List<String> selected = new ArrayList<>();
                for (Map.Entry<String, JCheckBox> entry : checkBoxes.entrySet()) {
                    if (entry.getValue().isSelected()) {
                        selected.add(entry.getKey());
                    }
                }
                if (selected.isEmpty()) {
                    JOptionPane.showMessageDialog(groupDialog, "Select at least one contact.");
                    return;
                }
                selected.add(currentUser.username);
                String membersStr = String.join(",", selected);
                if(networkClient != null) {
                    networkClient.sendMessage("", "CREATE_GROUP|" + groupName + "|" + currentUser.username + "|" + membersStr);
                }
                groups.put(groupName, new HashSet<>(selected));
                // You may also want to persist the group here if the server supports that.
                chatMainPanel.refreshContacts();
                groupDialog.dispose();
            });

            JPanel bottomPanel = new JPanel(new FlowLayout());
            bottomPanel.setBackground(DARK_BG);
            bottomPanel.add(createBtn);

            groupDialog.add(scrollPane, BorderLayout.CENTER);
            groupDialog.add(groupNamePanel, BorderLayout.NORTH);
            groupDialog.add(bottomPanel, BorderLayout.SOUTH);
            groupDialog.setVisible(true);
        }

        private JPanel createMessagePanel(Message m) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            panel.setBackground(DARKER_BG);
            String displayName = m.getSender().equals(currentUser.username) ? "You" : m.getSender();
            JLabel msgLabel;
            if (m.getType().equals("MSG") || m.getType().equals("GROUP_MSG") || m.getType().equals("GROUP_FILE")) {
                msgLabel = new JLabel(displayName + ": " + m.getContent());
            } else if (m.getType().equals("FILE")) {
                msgLabel = new JLabel(displayName + " sent a file: " + m.getContent());
            } else {
                msgLabel = new JLabel(displayName + ": " + m.getContent());
            }
            msgLabel.setForeground(LIGHT_TEXT);
            panel.add(msgLabel, BorderLayout.CENTER);

            JLabel statusLabel = new JLabel();
            statusLabel.setForeground(new Color(0, 255, 0));
            if (m.getSender().equals(currentUser.username)) {
                switch (m.getStatus()) {
                    case "DELIVERED": statusLabel.setText(" ✔"); break;
                    case "READ":      statusLabel.setText(" ✔✔"); break;
                    default:          statusLabel.setText(" …"); break;
                }
            }
            panel.add(statusLabel, BorderLayout.EAST);

            // Add a download button for file messages
            if (m.getType().equals("FILE") || m.getType().equals("GROUP_FILE")) {
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
                            JOptionPane.showMessageDialog(chatSessionPanel, "File downloaded to " + outFile.getAbsolutePath());
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
                String snippet = (m.getType().equals("FILE") || m.getType().equals("GROUP_FILE")) ? "[File: " + m.getContent() + "]" : m.getContent();
                if (snippet.length() > 20) {
                    snippet = snippet.substring(0, 20) + "...";
                }
                currentUser.unreadSnippets.put(contact, snippet);
            }
            db.saveMessagesForUser(currentUser.username, currentUser.chatHistory);
        }

        public void updateConversation(String contact, String displayText) {
            if (currentChatContact != null && currentChatContact.equals(contact)) {
                JLabel label = new JLabel(displayText);
                label.setForeground(LIGHT_TEXT);
                JPanel p = new JPanel(new BorderLayout());
                p.setBackground(DARK_BG);
                p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                p.add(label, BorderLayout.CENTER);
                conversationPanel.add(p);
                conversationPanel.revalidate();
                conversationPanel.repaint();
            }
        }
    }

    // ------------- NETWORK CLIENT -------------
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
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out.println(username);
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
                    String[] parts = line.split("\\|", 7);
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
                            if (chatMainPanel.currentChatContact == null || !chatMainPanel.currentChatContact.equals(sender)) {
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
                        if (chatMainPanel.currentChatContact != null && chatMainPanel.currentChatContact.equals(sender)) {
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
                            if (chatMainPanel.currentChatContact == null || !chatMainPanel.currentChatContact.equals(sender)) {
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
                        if (chatMainPanel.currentChatContact != null && chatMainPanel.currentChatContact.equals(sender)) {
                            sendMessage(sender, "ACK|" + msgId + "|READ");
                            m.setStatus("READ");
                        }
                    } else if (type.equals("GROUP_MSG")) {
                        if (parts.length < 5)
                            continue;
                        String msgId = parts[1];
                        String sender = parts[2];
                        String groupName = parts[3];
                        String content = parts[4];
                        Message m = new Message(msgId, sender, groupName, content, "GROUP_MSG", null);
                        m.setStatus("DELIVERED");
                        String localGroupKey = "Group:" + groupName;
                        if (currentUser != null && (chatMainPanel.currentChatContact == null || !chatMainPanel.currentChatContact.equals(localGroupKey))) {
                            int cnt = currentUser.unreadCounts.getOrDefault(localGroupKey, 0) + 1;
                            currentUser.unreadCounts.put(localGroupKey, cnt);
                            currentUser.unreadSnippets.put(localGroupKey, content.length() > 20 ? content.substring(0,20) + "..." : content);
                            chatMainPanel.refreshContacts();
                        }
                        currentUser.chatHistory.computeIfAbsent(localGroupKey, k -> new ArrayList<>()).add(m);
                        SwingUtilities.invokeLater(() -> {
                            chatMainPanel.updateConversation(localGroupKey, sender + " (in " + groupName + "): " + content + " ✔");
                        });
                        if (chatMainPanel.currentChatContact != null && chatMainPanel.currentChatContact.equals(localGroupKey)) {
                            sendMessage("", "ACK|" + msgId + "|READ");
                            m.setStatus("READ");
                        }
                    } else if (type.equals("GROUP_FILE")) {
                        if (parts.length < 6)
                            continue;
                        String msgId = parts[1];
                        String sender = parts[2];
                        String groupName = parts[3];
                        String filename = parts[4];
                        String base64data = parts[5];
                        Message m = new Message(msgId, sender, groupName, filename, "GROUP_FILE", base64data);
                        m.setStatus("DELIVERED");
                        String localGroupKey = "Group:" + groupName;
                        if (currentUser != null && (chatMainPanel.currentChatContact == null || !chatMainPanel.currentChatContact.equals(localGroupKey))) {
                            int cnt = currentUser.unreadCounts.getOrDefault(localGroupKey, 0) + 1;
                            currentUser.unreadCounts.put(localGroupKey, cnt);
                            currentUser.unreadSnippets.put(localGroupKey, filename);
                            chatMainPanel.refreshContacts();
                        }
                        currentUser.chatHistory.computeIfAbsent(localGroupKey, k -> new ArrayList<>()).add(m);
                        SwingUtilities.invokeLater(() -> {
                            chatMainPanel.updateConversation(localGroupKey, sender + " (in " + groupName + ") sent a file: " + filename + " ✔");
                        });
                        if (chatMainPanel.currentChatContact != null && chatMainPanel.currentChatContact.equals(localGroupKey)) {
                            sendMessage("", "ACK|" + msgId + "|READ");
                            m.setStatus("READ");
                        }
                    } else if (type.equals("GROUP_CREATED")) {
                        if (parts.length >= 3) {
                            String groupName = parts[1];
                            String membersStr = parts[2];
                            Set<String> memSet = new HashSet<>(Arrays.asList(membersStr.split(",")));
                            groups.put(groupName, memSet);
                            SwingUtilities.invokeLater(() -> {
                                chatMainPanel.refreshContacts();
                            });
                        }
                    } else if (type.equals("GROUP_UPDATE")) {
                        if (parts.length >= 4) {
                            String groupName = parts[1];
                            String updateType = parts[2];
                            String data = parts[3];
                            if (updateType.equals("NAME_CHANGED")) {
                                Set<String> mem = groups.get(groupName);
                                groups.remove(groupName);
                                groups.put(data, mem);
                                SwingUtilities.invokeLater(() -> {
                                    JOptionPane.showMessageDialog(ChatClient.this, "Group " + groupName + " renamed to " + data);
                                    chatMainPanel.refreshContacts();
                                });
                            } else if (updateType.equals("MEMBER_LEFT")) {
                                Set<String> mem = groups.get(groupName);
                                if (mem != null) {
                                    mem.remove(data);
                                }
                                SwingUtilities.invokeLater(() -> {
                                    chatMainPanel.refreshContacts();
                                });
                            } else if (updateType.equals("USER_ADDED")) {
                                Set<String> mem = groups.get(groupName);
                                if (mem != null) {
                                    mem.add(data);
                                }
                                SwingUtilities.invokeLater(() -> {
                                    chatMainPanel.refreshContacts();
                                });
                            }
                        }
                    } else if (type.equals("ACK")) {
                        if (parts.length < 3)
                            continue;
                        String msgId = parts[1];
                        String status = parts[2];
                        SwingUtilities.invokeLater(() -> {
                            System.out.println("Message " + msgId + " status updated: " + status);
                        });
                    } else if (type.equals("GROUP_INFO")) {
                        if (parts.length >= 3) {
                            String groupName = parts[1];
                            String memStr = parts[2];
                            Set<String> memSet = new HashSet<>(Arrays.asList(memStr.split(",")));
                            groups.put(groupName, memSet);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void close() {
            try {
                socket.close();
            } catch (IOException e) {}
        }
    }

    // ------------- SQL DATABASE HELPER -------------
    private class SQLDatabase {
        private Connection conn;
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
                    "username TEXT PRIMARY KEY, " +
                    "name TEXT, " +
                    "password TEXT, " +
                    "profile_photo TEXT)";
            String createMessages = "CREATE TABLE IF NOT EXISTS messages (" +
                    "message_id TEXT PRIMARY KEY, " +
                    "sender TEXT, " +
                    "recipient TEXT, " +
                    "content TEXT, " +
                    "type TEXT, " +
                    "file_data TEXT, " +
                    "status TEXT, " +
                    "timestamp INTEGER)";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createUsers);
                stmt.execute(createMessages);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        public Map<String, User> loadUsers() {
            Map<String, User> userMap = new HashMap<>();
            String query = "SELECT * FROM users";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    String username = rs.getString("username");
                    String name = rs.getString("name");
                    String password = rs.getString("password");
                    String photo = rs.getString("profile_photo");
                    User u = new User(name, username, password);
                    u.profilePhotoBase64 = photo;
                    userMap.put(username, u);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return userMap;
        }
        public void saveUser(User u) {
            String insert = "INSERT OR REPLACE INTO users(username, name, password, profile_photo) VALUES (?,?,?,?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insert)) {
                pstmt.setString(1, u.username);
                pstmt.setString(2, u.name);
                pstmt.setString(3, u.password);
                pstmt.setString(4, u.profilePhotoBase64);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        public void updateUser(User u) {
            saveUser(u);
        }
        public void saveMessage(Message m) {
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
        public void saveMessagesForUser(String username, Map<String, List<Message>> chatHistory) {
            String delete = "DELETE FROM messages WHERE sender = ? OR recipient = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(delete)) {
                pstmt.setString(1, username);
                pstmt.setString(2, username);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            for (List<Message> msgs : chatHistory.values()) {
                for (Message m : msgs) {
                    saveMessage(m);
                }
            }
        }
        public Map<String, List<Message>> loadMessagesForUser(String username) {
            Map<String, List<Message>> history = new HashMap<>();
            String query = "SELECT * FROM messages WHERE sender = ? OR recipient = ? ORDER BY timestamp ASC";
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
                        Message m = new Message(msgId, sender, recipient, content, type, fileData);
                        m.setStatus(status);
                        m.setTimestamp(timestamp);
                        String contact = sender.equals(username) ? recipient : sender;
                        history.computeIfAbsent(contact, k -> new ArrayList<>()).add(m);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return history;
        }
    }

    // ------------- MAIN METHOD -------------
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> {
            ChatClient client = new ChatClient();
            client.setVisible(true);
        });
    }
}
