import com.formdev.flatlaf.FlatDarkLaf;
import org.pushingpixels.trident.Timeline;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;

public class ChatClient extends JFrame {

    // ------------- Color and Font Constants -------------
    private static final Color DARK_BG       = new Color(30, 30, 30);
    private static final Color DARKER_BG     = new Color(20, 20, 20);
    private static final Color LIGHT_TEXT    = new Color(220, 220, 220);
    private static final Color ACCENT_COLOR  = new Color(100, 255, 218);
    private static final Color BUTTON_BG     = new Color(80, 180, 255);
    private static final Color BUTTON_BG_HOVER = new Color(255, 255, 255);
    private static final Color BUTTON_TEXT   = Color.WHITE;
    private static final Color BUTTON_TEXT_HOVER = Color.BLACK;
    private static final Color FIELD_BG      = new Color(60, 60, 60);
    private static final Color DATE_BG       = new Color(80, 80, 80);

    private static final Font TITLE_FONT  = new Font("SansSerif", Font.BOLD, 36);
    private static final Font LABEL_FONT  = new Font("SansSerif", Font.BOLD, 16);
    private static final Font FIELD_FONT  = new Font("SansSerif", Font.PLAIN, 16);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 14);

    // ------------- Database Info -------------
    private static final String DB_URL = "jdbc:sqlite:chatapp.db";

    // ------------- Data Models -------------
    public static class User implements Serializable {
        private static final long serialVersionUID = 1L;
        public String name, username, password, profilePhotoBase64;
        public Map<String, List<Message>> chatHistory = new HashMap<>();
        public Map<String, Integer> unreadCounts = new HashMap<>();
        public Map<String, String> unreadSnippets = new HashMap<>();

        public User(String name, String username, String password) {
            this.name = name;
            this.username = username;
            this.password = password;
        }
    }

    public static class Message implements Serializable {
        private static final long serialVersionUID = 1L;
        private String messageId, sender, recipient, content, type, fileData, status;
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
    // Local mapping for groups: groupName -> Set of members (should be updated via server notifications)
    private Map<String, Set<String>> groups = new HashMap<>();

    // ------------- Constructor -------------
    public ChatClient() {
        setTitle("Dark Mode Chat App");
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

    // ------------- Helper Methods -------------
    public void styleTextField(JTextField field) {
        field.setFont(FIELD_FONT);
        field.setBackground(FIELD_BG);
        field.setForeground(LIGHT_TEXT);
        field.setCaretColor(LIGHT_TEXT);
        field.setBorder(new LineBorder(ACCENT_COLOR, 1));
    }

    public void styleTextField(JPasswordField field) {
        field.setFont(FIELD_FONT);
        field.setBackground(FIELD_BG);
        field.setForeground(LIGHT_TEXT);
        field.setCaretColor(LIGHT_TEXT);
        field.setBorder(new LineBorder(ACCENT_COLOR, 1));
    }

    public void styleButton(JButton button) {
        button.setFont(BUTTON_FONT);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorder(new RoundedBorder(10));
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            public void paint(Graphics g, JComponent c) {
                AbstractButton b = (AbstractButton) c;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ButtonModel model = b.getModel();
                int arc = 20;
                Color bg = BUTTON_BG;
                Color fg = BUTTON_TEXT;
                if (model.isRollover()) {
                    bg = BUTTON_BG_HOVER;
                    fg = BUTTON_TEXT_HOVER;
                }
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, b.getWidth(), b.getHeight(), arc, arc);
                g2.setColor(ACCENT_COLOR);
                g2.drawRoundRect(0, 0, b.getWidth() - 1, b.getHeight() - 1, arc, arc);
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                Rectangle r = new Rectangle(0, 0, b.getWidth(), b.getHeight());
                String text = b.getText();
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getAscent();
                int x = (r.width - textWidth) / 2;
                int y = (r.height + textHeight) / 2 - 2;
                g2.drawString(text, x, y);
                g2.dispose();
            }
        });
    }

    public void styleInputArea(JTextArea area) {
        area.setFont(FIELD_FONT);
        area.setBackground(FIELD_BG);
        area.setForeground(LIGHT_TEXT);
        area.setCaretColor(LIGHT_TEXT);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new RoundedBorder(10));
        area.setRows(1);
    }

    public void showLoader(String message) {
        JDialog loader = new JDialog(this, "Loading", true);
        loader.setSize(300, 100);
        loader.setLocationRelativeTo(this);
        loader.setLayout(new BorderLayout());
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setForeground(LIGHT_TEXT);
        label.setFont(LABEL_FONT);
        loader.getContentPane().setBackground(DARK_BG);
        loader.add(label, BorderLayout.CENTER);
        new javax.swing.Timer(2000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loader.dispose();
            }
        }).start();
        loader.setVisible(true);
    }

    public void scrollToBottom(JScrollPane scrollPane) {
        SwingUtilities.invokeLater(() -> {
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            verticalBar.setValue(verticalBar.getMaximum());
        });
    }

    public class RoundedBorder extends AbstractBorder {
        private int radius;
        public RoundedBorder(int radius) { this.radius = radius; }
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.getBackground());
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }
        public Insets getBorderInsets(Component c) {
            return new Insets(radius + 1, radius + 1, radius + 1, radius + 1);
        }
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.top = insets.right = insets.bottom = radius + 1;
            return insets;
        }
    }

    public class ChatBubble extends JPanel {
        private String message;
        private String timeText;
        private Color bubbleColor;
        private float alpha = 0f;
        private int offset;

        public ChatBubble(String message, long timestamp, Color bubbleColor, boolean isSelf) {
            this.message = message;
            this.bubbleColor = bubbleColor;
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
            this.timeText = sdf.format(new java.util.Date(timestamp));
            this.offset = isSelf ? 50 : -50;
            setOpaque(false);
            setBorder(new EmptyBorder(8, 12, 20, 12));
        }
        
        @Override
        public Dimension getPreferredSize() {
            Graphics g = getGraphics();
            if (g == null) { g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).getGraphics(); }
            FontMetrics fm = g.getFontMetrics(getFont());
            int maxWidth = 300;
            int lineHeight = fm.getHeight();
            String[] words = message.split(" ");
            int currentLineWidth = 0;
            int lines = 1;
            for (String word : words) {
                int wordWidth = fm.stringWidth(word + " ");
                if (currentLineWidth + wordWidth > maxWidth - 20) {
                    lines++;
                    currentLineWidth = wordWidth;
                } else {
                    currentLineWidth += wordWidth;
                }
            }
            int width = Math.min(maxWidth, currentLineWidth + 20);
            int height = lines * lineHeight + 20;
            return new Dimension(width, height);
        }

        public void setAlpha(float alpha) { this.alpha = alpha; repaint(); }
        public void setOffset(int offset) { this.offset = offset; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(offset, 0);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = 20;
            int width = getWidth() - Math.abs(offset);
            int height = getHeight();
            g2.setColor(bubbleColor);
            g2.fillRoundRect(0, 0, width, height, arc, arc);
            g2.setColor(getForeground());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int textY = fm.getAscent() + 5;
            g2.drawString(message, 10, textY);
            Font timeFont = getFont().deriveFont(Font.ITALIC, 10f);
            g2.setFont(timeFont);
            FontMetrics tfm = g2.getFontMetrics(timeFont);
            int timeWidth = tfm.stringWidth(timeText);
            int timeY = height - 5;
            g2.drawString(timeText, width - timeWidth - 10, timeY);
            g2.dispose();
        }
    }

    public class DateHeader extends JPanel {
        private String dateText;
        public DateHeader(String dateText) {
            this.dateText = dateText;
            setOpaque(false);
            setPreferredSize(new Dimension(300, 30));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = 15;
            int width = getWidth();
            int height = getHeight();
            g2.setColor(DATE_BG);
            g2.fillRoundRect(0, 0, width, height, arc, arc);
            g2.setColor(LIGHT_TEXT);
            Font font = new Font("SansSerif", Font.PLAIN, 10);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(dateText);
            int textX = (width - textWidth) / 2;
            int textY = (height - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(dateText, textX, textY);
            g2.dispose();
        }
    }

    private class LoginPanel extends JPanel {
        public LoginPanel() {
            setBackground(DARK_BG);
            setLayout(new BorderLayout());
            JLabel title = new JLabel("Welcome Back", SwingConstants.CENTER);
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
            ChatClient.this.styleTextField(usernameField);
            formPanel.add(usernameField, gbc);
            gbc.gridx = 0; gbc.gridy = 1;
            JLabel passLabel = new JLabel("Password:");
            passLabel.setFont(LABEL_FONT);
            passLabel.setForeground(LIGHT_TEXT);
            formPanel.add(passLabel, gbc);
            gbc.gridx = 1;
            JPasswordField passwordField = new JPasswordField(20);
            ChatClient.this.styleTextField(passwordField);
            formPanel.add(passwordField, gbc);
            gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
            JButton loginButton = new JButton("LOGIN");
            ChatClient.this.styleButton(loginButton);
            formPanel.add(loginButton, gbc);
            gbc.gridy = 3;
            JButton toRegisterButton = new JButton("REGISTER");
            ChatClient.this.styleButton(toRegisterButton);
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
                        ChatClient.this.showLoader("Logging in...");
                        currentUser = user;
                        // Load individual and group messages (group messages are now loaded by checking type LIKE 'GROUP_%')
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
                ChatClient.this.showLoader("Opening Registration...");
                cardLayout.show(mainPanel, "register");
            });
        }
    }

    private class RegistrationPanel extends JPanel {
        public RegistrationPanel() {
            setBackground(DARK_BG);
            setLayout(new BorderLayout());
            JLabel title = new JLabel("Create Account", SwingConstants.CENTER);
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
            ChatClient.this.styleTextField(nameField);
            formPanel.add(nameField, gbc);
            gbc.gridx = 0; gbc.gridy = 1;
            JLabel userLabel = new JLabel("Username:");
            userLabel.setFont(LABEL_FONT);
            userLabel.setForeground(LIGHT_TEXT);
            formPanel.add(userLabel, gbc);
            gbc.gridx = 1;
            JTextField usernameField = new JTextField(20);
            ChatClient.this.styleTextField(usernameField);
            formPanel.add(usernameField, gbc);
            gbc.gridx = 0; gbc.gridy = 2;
            JLabel passLabel = new JLabel("Password:");
            passLabel.setFont(LABEL_FONT);
            passLabel.setForeground(LIGHT_TEXT);
            formPanel.add(passLabel, gbc);
            gbc.gridx = 1;
            JPasswordField passwordField = new JPasswordField(20);
            ChatClient.this.styleTextField(passwordField);
            formPanel.add(passwordField, gbc);
            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
            JButton registerButton = new JButton("REGISTER");
            ChatClient.this.styleButton(registerButton);
            formPanel.add(registerButton, gbc);
            gbc.gridy = 4;
            JButton backToLoginButton = new JButton("BACK TO LOGIN");
            ChatClient.this.styleButton(backToLoginButton);
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
                ChatClient.this.showLoader("Registering...");
                JOptionPane.showMessageDialog(this, "Registration successful! Please login.", "Success", JOptionPane.INFORMATION_MESSAGE);
                cardLayout.show(mainPanel, "login");
            });
            backToLoginButton.addActionListener(e -> {
                ChatClient.this.showLoader("Going back to Login...");
                cardLayout.show(mainPanel, "login");
            });
        }
    }

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
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0; gbc.gridy = 0;
            JLabel nameLabel = new JLabel("Name:");
            nameLabel.setFont(LABEL_FONT);
            nameLabel.setForeground(LIGHT_TEXT);
            formPanel.add(nameLabel, gbc);
            gbc.gridx = 1;
            nameField = new JTextField(20);
            ChatClient.this.styleTextField(nameField);
            formPanel.add(nameField, gbc);
            gbc.gridx = 0; gbc.gridy = 1;
            JLabel userLabel = new JLabel("Username:");
            userLabel.setFont(LABEL_FONT);
            userLabel.setForeground(LIGHT_TEXT);
            formPanel.add(userLabel, gbc);
            gbc.gridx = 1;
            usernameField = new JTextField(20);
            ChatClient.this.styleTextField(usernameField);
            formPanel.add(usernameField, gbc);
            gbc.gridx = 0; gbc.gridy = 2;
            JLabel passLabel = new JLabel("Password:");
            passLabel.setFont(LABEL_FONT);
            passLabel.setForeground(LIGHT_TEXT);
            formPanel.add(passLabel, gbc);
            gbc.gridx = 1;
            passwordField = new JPasswordField(20);
            ChatClient.this.styleTextField(passwordField);
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
            ChatClient.this.styleButton(loadPhotoButton);
            formPanel.add(loadPhotoButton, gbc);
            gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
            saveButton = new JButton("SAVE CHANGES");
            ChatClient.this.styleButton(saveButton);
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
                ChatClient.this.showLoader("Saving Profile...");
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
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(DARKER_BG);
            headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            headerLabel = new JLabel("Welcome, ...", SwingConstants.LEFT);
            headerLabel.setFont(TITLE_FONT);
            headerLabel.setForeground(ACCENT_COLOR);
            headerPanel.add(headerLabel, BorderLayout.WEST);
            JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            rightPanel.setOpaque(false);
            JButton refreshChatButton = new JButton("REFRESH CHAT");
            ChatClient.this.styleButton(refreshChatButton);
            refreshChatButton.addActionListener(e -> {
                users = db.loadUsers();
                refreshContacts();
                refreshChatHistory();
                JOptionPane.showMessageDialog(ChatMainPanel.this, "Chats Refreshed!");
            });
            JButton profileButton = new JButton("PROFILE");
            ChatClient.this.styleButton(profileButton);
            profileButton.addActionListener(e -> {
                if (currentUser != null) {
                    profilePanel.loadProfileData();
                    cardLayout.show(mainPanel, "profile");
                }
            });
            JButton logoutButton = new JButton("LOGOUT");
            ChatClient.this.styleButton(logoutButton);
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
            ChatClient.this.styleButton(createGroupButton);
            createGroupButton.addActionListener(e -> openCreateGroupDialog());
            JPanel leftPanel = new JPanel(new BorderLayout());
            leftPanel.add(createGroupButton, BorderLayout.NORTH);
            leftPanel.add(contactsScroll, BorderLayout.CENTER);
            chatSessionPanel = new JPanel(new BorderLayout());
            chatSessionPanel.setBackground(DARKER_BG);
            chatSessionPanel.setBorder(BorderFactory.createTitledBorder(new LineBorder(ACCENT_COLOR), "Conversation"));
            contactsList.addMouseListener(new MouseAdapter() {
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
            new javax.swing.Timer(5000, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    users = db.loadUsers();
                    refreshContacts();
                    refreshChatHistory();
                }
            }).start();
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
                        if (!snippet.isEmpty()) { display += " - " + snippet; }
                    }
                    contactsModel.addElement(display);
                }
            }
            for (String groupName : groups.keySet()) {
                contactsModel.addElement("Group: " + groupName);
            }
        }

        public void refreshChatHistory() {
            // Optionally update conversation panel if needed.
        }

        private JScrollPane createInputAreaPanel(JPanel parentPanel, java.util.function.Consumer<String> sendAction) {
            JTextArea inputArea = new JTextArea(1, 30);
            styleInputArea(inputArea);
            JScrollPane inputScroll = new JScrollPane(inputArea);
            inputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            inputScroll.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            InputMap im = inputArea.getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap am = inputArea.getActionMap();
            im.put(KeyStroke.getKeyStroke("ENTER"), "sendMessage");
            im.put(KeyStroke.getKeyStroke("shift ENTER"), "insertNewLine");
            am.put("sendMessage", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    sendAction.accept(inputArea.getText().trim());
                    inputArea.setText("");
                    inputArea.setRows(1);
                }
            });
            am.put("insertNewLine", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    inputArea.append("\n");
                    inputArea.setRows(inputArea.getLineCount());
                }
            });
            return inputScroll;
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
            final JScrollPane convScroll = new JScrollPane(conversationPanel);
            convScroll.setBorder(null);
            convScroll.getViewport().setBackground(DARK_BG);
            List<Message> history = currentUser.chatHistory.get(contact);
            if (history != null) {
                Collections.sort(history, Comparator.comparingLong(Message::getTimestamp));
                String lastDate = null;
                for (Message m : history) {
                    String msgDate = new SimpleDateFormat("MMM dd, yyyy").format(new java.util.Date(m.getTimestamp()));
                    if (lastDate == null || !lastDate.equals(msgDate)) {
                        conversationPanel.add(new DateHeader(msgDate));
                        lastDate = msgDate;
                    }
                    conversationPanel.add(createMessagePanel(m));
                }
            }
            scrollToBottom(convScroll);
            JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
            inputPanel.setBackground(DARKER_BG);
            final JScrollPane inputScroll = createInputAreaPanel(inputPanel, msgText -> {
                if (!msgText.isEmpty()) {
                    String msgId = currentUser.username + "-" + messageIdGenerator.getAndIncrement();
                    Message msg = new Message(msgId, currentUser.username, contact, msgText, "MSG", null);
                    msg.setStatus("PENDING");
                    addMessageToHistory(contact, msg);
                    String msgDate = new SimpleDateFormat("MMM dd, yyyy").format(new java.util.Date(msg.getTimestamp()));
                    Component last = conversationPanel.getComponentCount() > 0 ? conversationPanel.getComponent(conversationPanel.getComponentCount() - 1) : null;
                    String lastHeader = (last instanceof DateHeader) ? ((DateHeader) last).dateText : null;
                    if (lastHeader == null || !lastHeader.equals(msgDate)) {
                        conversationPanel.add(new DateHeader(msgDate));
                    }
                    conversationPanel.add(createMessagePanel(msg));
                    conversationPanel.revalidate();
                    conversationPanel.repaint();
                    if (networkClient != null) {
                        networkClient.sendMessage(contact, "MSG|" + msgId + "|" + currentUser.username + "|" + contact + "|" + msgText);
                    }
                    scrollToBottom(convScroll);
                }
            });
            JButton sendMsgButton = new JButton("Send");
            ChatClient.this.styleButton(sendMsgButton);
            JButton sendFileButton = new JButton("Send File");
            ChatClient.this.styleButton(sendFileButton);
            inputPanel.add(inputScroll);
            inputPanel.add(sendMsgButton);
            inputPanel.add(sendFileButton);
            JButton closeChatButton = new JButton("Close Chat");
            ChatClient.this.styleButton(closeChatButton);
            inputPanel.add(closeChatButton);
            closeChatButton.addActionListener(e -> {
                chatSessionPanel.removeAll();
                chatSessionPanel.revalidate();
                chatSessionPanel.repaint();
                currentChatContact = null;
            });
            sendMsgButton.addActionListener(e -> {
                ActionMap amLocal = ((JTextArea) inputScroll.getViewport().getView()).getActionMap();
                amLocal.get("sendMessage").actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
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
                        scrollToBottom(convScroll);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(chatSessionPanel, "Error reading file: " + ex.getMessage());
                    }
                }
            });
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
            chatSessionPanel.add(convScroll, BorderLayout.CENTER);
            chatSessionPanel.add(inputPanel, BorderLayout.SOUTH);
            chatSessionPanel.revalidate();
            chatSessionPanel.repaint();
        }

        public void openGroupChatSession(String groupName) {
            currentChatContact = "Group:" + groupName;
            chatSessionPanel.removeAll();
            chatSessionPanel.setLayout(new BorderLayout());
            JPanel groupHeader = new JPanel(new BorderLayout());
            groupHeader.setBackground(DARKER_BG);
            JLabel groupLabel = new JLabel("Group: " + groupName);
            groupLabel.setFont(TITLE_FONT);
            groupLabel.setForeground(ACCENT_COLOR);
            groupHeader.add(groupLabel, BorderLayout.WEST);
            groupLabel.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    MouseEvent me = (MouseEvent) e;
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
                    addUserItem.addActionListener(ae -> {
                        Set<String> currentMembers = groups.get(groupName);
                        List<String> availableUsers = new ArrayList<>();
                        for (String uname : users.keySet()) {
                            if (!uname.equals(currentUser.username) && (currentMembers == null || !currentMembers.contains(uname))) {
                                availableUsers.add(uname);
                            }
                        }
                        if (availableUsers.isEmpty()) {
                            JOptionPane.showMessageDialog(ChatMainPanel.this, "No users available to add.");
                            return;
                        }
                        JPanel panel = new JPanel();
                        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                        Map<String, JCheckBox> checkboxMap = new HashMap<>();
                        for (String user : availableUsers) {
                            JCheckBox cb = new JCheckBox(user);
                            cb.setForeground(LIGHT_TEXT);
                            cb.setBackground(DARK_BG);
                            panel.add(cb);
                            checkboxMap.put(user, cb);
                        }
                        int result = JOptionPane.showConfirmDialog(ChatMainPanel.this, panel, "Select users to add", JOptionPane.OK_CANCEL_OPTION);
                        if (result == JOptionPane.OK_OPTION) {
                            for (Map.Entry<String, JCheckBox> entry : checkboxMap.entrySet()) {
                                if (entry.getValue().isSelected()) {
                                    networkClient.sendMessage("", "ADD_TO_GROUP|" + groupName + "|" + currentUser.username + "|" + entry.getKey());
                                }
                            }
                        }
                    });
                    menu.add(leaveItem);
                    menu.add(changeNameItem);
                    menu.add(showMembersItem);
                    menu.add(addUserItem);
                    menu.show(groupLabel, me.getX(), me.getY());
                }
            });
            chatSessionPanel.add(groupHeader, BorderLayout.NORTH);
            conversationPanel = new JPanel();
            conversationPanel.setLayout(new BoxLayout(conversationPanel, BoxLayout.Y_AXIS));
            conversationPanel.setBackground(DARK_BG);
            final JScrollPane convScroll = new JScrollPane(conversationPanel);
            convScroll.setBorder(null);
            convScroll.getViewport().setBackground(DARK_BG);
            List<Message> history = currentUser.chatHistory.get("Group:" + groupName);
            if (history != null) {
                Collections.sort(history, Comparator.comparingLong(Message::getTimestamp));
                String lastDate = null;
                for (Message m : history) {
                    String msgDate = new SimpleDateFormat("MMM dd, yyyy").format(new java.util.Date(m.getTimestamp()));
                    if (lastDate == null || !lastDate.equals(msgDate)) {
                        conversationPanel.add(new DateHeader(msgDate));
                        lastDate = msgDate;
                    }
                    conversationPanel.add(createMessagePanel(m));
                }
            }
            scrollToBottom(convScroll);
            JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
            inputPanel.setBackground(DARK_BG);
            final JScrollPane inputScroll = createInputAreaPanel(inputPanel, msgText -> {
                if (!msgText.isEmpty()) {
                    String msgId = currentUser.username + "-" + messageIdGenerator.getAndIncrement();
                    Message msg = new Message(msgId, currentUser.username, groupName, msgText, "GROUP_MSG", null);
                    msg.setStatus("PENDING");
                    addMessageToHistory("Group:" + groupName, msg);
                    String msgDate = new SimpleDateFormat("MMM dd, yyyy").format(new java.util.Date(msg.getTimestamp()));
                    Component last = conversationPanel.getComponentCount() > 0 ? conversationPanel.getComponent(conversationPanel.getComponentCount() - 1) : null;
                    String lastHeader = (last instanceof DateHeader) ? ((DateHeader) last).dateText : null;
                    if (lastHeader == null || !lastHeader.equals(msgDate)) {
                        conversationPanel.add(new DateHeader(msgDate));
                    }
                    conversationPanel.add(createMessagePanel(msg));
                    conversationPanel.revalidate();
                    conversationPanel.repaint();
                    if (networkClient != null) {
                        networkClient.sendMessage("", "GROUP_MSG|" + msgId + "|" + currentUser.username + "|" + groupName + "|" + msgText);
                    }
                    scrollToBottom(convScroll);
                }
            });
            JButton sendMsgButton = new JButton("Send");
            ChatClient.this.styleButton(sendMsgButton);
            JButton sendFileButton = new JButton("Send File");
            ChatClient.this.styleButton(sendFileButton);
            sendMsgButton.addActionListener(e -> {
                ActionMap amLocal = ((JTextArea) inputScroll.getViewport().getView()).getActionMap();
                amLocal.get("sendMessage").actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
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
                        scrollToBottom(convScroll);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(chatSessionPanel, "Error reading file: " + ex.getMessage());
                    }
                }
            });
            JButton closeChatButton = new JButton("Close Chat");
            ChatClient.this.styleButton(closeChatButton);
            closeChatButton.addActionListener(e -> {
                chatSessionPanel.removeAll();
                chatSessionPanel.revalidate();
                chatSessionPanel.repaint();
                currentChatContact = null;
            });
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
            bottomPanel.setBackground(DARK_BG);
            bottomPanel.add(inputScroll);
            bottomPanel.add(sendMsgButton);
            bottomPanel.add(sendFileButton);
            bottomPanel.add(closeChatButton);
            chatSessionPanel.add(convScroll, BorderLayout.CENTER);
            chatSessionPanel.add(bottomPanel, BorderLayout.SOUTH);
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
            ChatClient.this.styleTextField(groupNameField);
            groupNamePanel.add(nameLabel);
            groupNamePanel.add(groupNameField);
            JButton createBtn = new JButton("Create");
            ChatClient.this.styleButton(createBtn);
            createBtn.addActionListener(e -> {
                String groupName = groupNameField.getText().trim();
                if (groupName.isEmpty()) {
                    JOptionPane.showMessageDialog(groupDialog, "Please enter a group name.");
                    return;
                }
                List<String> selected = new ArrayList<>();
                for (Map.Entry<String, JCheckBox> entry : checkBoxes.entrySet()) {
                    if (entry.getValue().isSelected()) { selected.add(entry.getKey()); }
                }
                if (selected.isEmpty()) {
                    JOptionPane.showMessageDialog(groupDialog, "Select at least one contact.");
                    return;
                }
                selected.add(currentUser.username);
                String membersStr = String.join(",", selected);
                if (networkClient != null) {
                    networkClient.sendMessage("", "CREATE_GROUP|" + groupName + "|" + currentUser.username + "|" + membersStr);
                }
                groups.put(groupName, new HashSet<>(selected));
                refreshContacts();
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
            boolean isSelf = m.getSender().equals(currentUser.username);
            Color bubbleColor = isSelf ? new Color(100, 200, 255) : new Color(200, 200, 200);
            ChatBubble bubble = new ChatBubble(isSelf ? "You: " + m.getContent() : m.getSender() + ": " + m.getContent(),
                    m.getTimestamp(), bubbleColor, isSelf);
            bubble.setFont(new Font("SansSerif", Font.PLAIN, 14));
            bubble.setForeground(Color.BLACK);
            JPanel panel = new JPanel(new BorderLayout());
            panel.setOpaque(false);
            panel.setBorder(new EmptyBorder(5, 5, 5, 5));
            panel.add(bubble, BorderLayout.CENTER);
            Timeline fadeTimeline = new Timeline(bubble);
            fadeTimeline.addPropertyToInterpolate("alpha", 0f, 1f);
            fadeTimeline.setDuration(500);
            fadeTimeline.play();
            Timeline slideTimeline = new Timeline(bubble);
            slideTimeline.addPropertyToInterpolate("offset", bubble.offset, 0);
            slideTimeline.setDuration(500);
            slideTimeline.play();
            if (m.getType().equals("FILE") || m.getType().equals("GROUP_FILE")) {
                JButton downloadBtn = new JButton("Download");
                ChatClient.this.styleButton(downloadBtn);
                downloadBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
                downloadBtn.addActionListener(e -> {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setSelectedFile(new File(m.getContent()));
                    int res = chooser.showSaveDialog(panel);
                    if (res == JFileChooser.APPROVE_OPTION) {
                        File outFile = chooser.getSelectedFile();
                        try {
                            byte[] data = Base64.getDecoder().decode(m.getFileData());
                            Files.write(outFile.toPath(), data);
                            JOptionPane.showMessageDialog(panel, "File downloaded to " + outFile.getAbsolutePath());
                            downloadBtn.setEnabled(false);
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(panel, "Error saving file: " + ex.getMessage());
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
                String snippet = (m.getType().equals("FILE") || m.getType().equals("GROUP_FILE"))
                        ? "[File: " + m.getContent() + "]" : m.getContent();
                if (snippet.length() > 20) { snippet = snippet.substring(0, 20) + "..."; }
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
                p.setBorder(new EmptyBorder(5, 5, 5, 5));
                p.add(label, BorderLayout.CENTER);
                conversationPanel.add(p);
                conversationPanel.revalidate();
                conversationPanel.repaint();
            }
        }
    }

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
            if (out != null) { out.println(message); }
        }
        public void run() {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    String[] parts = line.split("\\|", 7);
                    if (parts.length < 1) continue;
                    String type = parts[0];
                    if (type.equals("MSG")) {
                        if (parts.length < 5) continue;
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
                        if (parts.length < 6) continue;
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
                        if (parts.length < 5) continue;
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
                            currentUser.unreadSnippets.put(localGroupKey, content.length() > 20 ? content.substring(0, 20) + "..." : content);
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
                        if (parts.length < 6) continue;
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
                            SwingUtilities.invokeLater(() -> { chatMainPanel.refreshContacts(); });
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
                                if (mem != null) { mem.remove(data); }
                                SwingUtilities.invokeLater(() -> { chatMainPanel.refreshContacts(); });
                            } else if (updateType.equals("USER_ADDED")) {
                                Set<String> mem = groups.get(groupName);
                                if (mem != null) { mem.add(data); }
                                SwingUtilities.invokeLater(() -> { chatMainPanel.refreshContacts(); });
                            }
                        }
                    } else if (type.equals("ACK")) {
                        if (parts.length < 3) continue;
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
            try { socket.close(); } catch (IOException e) {}
        }
    }

    private class SQLDatabase {
        private Connection conn;
        public SQLDatabase() {
            try {
                Class.forName("org.sqlite.JDBC");
                conn = DriverManager.getConnection(DB_URL);
            } catch (Exception e) { e.printStackTrace(); }
        }
        public void initialize() {
            String createUsers = "CREATE TABLE IF NOT EXISTS users (" +
                    "username TEXT PRIMARY KEY, name TEXT, password TEXT, profile_photo TEXT)";
            String createMessages = "CREATE TABLE IF NOT EXISTS messages (" +
                    "message_id TEXT PRIMARY KEY, sender TEXT, recipient TEXT, content TEXT, type TEXT, file_data TEXT, status TEXT, timestamp INTEGER)";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createUsers);
                stmt.execute(createMessages);
            } catch (SQLException e) { e.printStackTrace(); }
        }
        public Map<String, User> loadUsers() {
            Map<String, User> userMap = new HashMap<>();
            String query = "SELECT * FROM users";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    String username = rs.getString("username");
                    String name = rs.getString("name");
                    String password = rs.getString("password");
                    String photo = rs.getString("profile_photo");
                    User u = new User(name, username, password);
                    u.profilePhotoBase64 = photo;
                    userMap.put(username, u);
                }
            } catch (SQLException e) { e.printStackTrace(); }
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
            } catch (SQLException e) { e.printStackTrace(); }
        }
        public void updateUser(User u) { saveUser(u); }
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
            } catch (SQLException e) { e.printStackTrace(); }
        }
        public void saveMessagesForUser(String username, Map<String, List<Message>> chatHistory) {
            String delete = "DELETE FROM messages WHERE sender = ? OR recipient = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(delete)) {
                pstmt.setString(1, username);
                pstmt.setString(2, username);
                pstmt.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
            for (List<Message> msgs : chatHistory.values()) {
                for (Message m : msgs) { saveMessage(m); }
            }
        }
        // Modified query to load both individual and group messages.
        public Map<String, List<Message>> loadMessagesForUser(String username) {
            Map<String, List<Message>> history = new HashMap<>();
            // Note: We load group messages as well by checking for messages where type starts with 'GROUP_'
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
                        Message m = new Message(msgId, sender, recipient, content, type, fileData);
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
            } catch (SQLException e) { e.printStackTrace(); }
            return history;
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(new FlatDarkLaf()); }
        catch (UnsupportedLookAndFeelException e) { e.printStackTrace(); }
        SwingUtilities.invokeLater(() -> {
            ChatClient client = new ChatClient();
            client.setVisible(true);
        });
    }
}
