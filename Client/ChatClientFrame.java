package chatting;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;

public class ChatClientFrame extends JFrame {

    public CardLayout cardLayout;
    public JPanel mainPanel;
    public ChatMainPanel chatMainPanel;
    public ProfilePanel profilePanel;
    public NetworkClient networkClient;
    public final String SERVER_ADDRESS = "localhost";
    public final int SERVER_PORT = 12345;
    public AtomicLong messageIdGenerator = new AtomicLong(System.currentTimeMillis());
    public SQLDatabase db;
    public Map<String, UserData.User> users;
    public UserData.User currentUser;
    public Map<String, java.util.Set<String>> groups = new HashMap<>();
    public Friend friendManager;

    // ------------- Color and Font Constants (Moved to UIStyles.java) -------------
    // ------------- Database Info (Moved to SQLDatabase.java) -------------
    // ------------- Data Models (Moved to UserData.java and MessageData.java) -------------
    // ------------- UI Components (Defined in their respective panel classes) -------------
    // ------------- Network Components (Moved to NetworkClient.java) -------------
    // ------------- Others -------------

    // ------------- Constructor -------------
    public ChatClientFrame() {
        setTitle("Dark Mode Chat App");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        db = new SQLDatabase();
        db.initialize();
        users = db.loadUsers();


        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(UIStyles.DARK_BG);

        LoginPanel loginPanel = new LoginPanel(this);
        mainPanel.add(loginPanel, "login");

        RegistrationPanel registrationPanel = new RegistrationPanel(this);
        mainPanel.add(registrationPanel, "register");

        chatMainPanel = new ChatMainPanel(this);
        mainPanel.add(chatMainPanel, "chat");

        profilePanel = new ProfilePanel(this);
        mainPanel.add(profilePanel, "profile");

        add(mainPanel);
        cardLayout.show(mainPanel, "login");
    }

    //friend--------------------------------------------------------
    
    public void initializeFriendManager() {
        
        if (this.currentUser != null) {
            friendManager = new Friend(this.currentUser.getUsername(), db);
            System.out.println("Friend manager initialized for: " + currentUser.getUsername());
        }
        else {
            System.out.println("Current user is null, cannot initialize friend manager.");
        }
    }
    

    public void addFriend(String friendUsername) {
        //debugging
        System.out.println("Adding friend from frame : " + friendUsername);
        if (friendUsername == null || friendUsername.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Friend username cannot be empty.");
            return;
        }
        System.out.println("Adding friend from after: " + friendUsername);
        System.out.println("Friend manager: " + friendManager);
        if (friendManager != null && friendManager.addFriend(friendUsername)) {
            JOptionPane.showMessageDialog(this, friendUsername + " added as a friend.");
        }else{
            JOptionPane.showMessageDialog(this, "Failed to add " + friendUsername + " as a friend.");
        }
    }

    public void removeFriend(String friendUsername) {
        if (friendManager != null && friendManager.removeFriend(friendUsername)) {
            JOptionPane.showMessageDialog(this, friendUsername + " removed from friends.");
        }
    }

    public List<String> getFriendsList() {
        return friendManager != null ? friendManager.getFriends() : null;
    }

    public boolean isFriend(String friendUsername) {
        return friendManager != null && friendManager.isFriend(friendUsername);
    }

    // ------------- Helper Methods (Moved to UIStyles.java or remain here for app logic) -------------
    
    public void styleTextField(JTextField field) {
        UIStyles.styleTextField(field);
    }

    public void styleTextField(JPasswordField field) {
        UIStyles.styleTextField(field);
    }

    public void styleButton(JButton button) {
        UIStyles.styleButton(button);
    }

    public void styleInputArea(JTextArea area) {
        UIStyles.styleInputArea(area);
    }

    public void showLoader(String message) {
        JDialog loader = new JDialog(this, "Loading", true);
        loader.setSize(300, 100);
        loader.setLocationRelativeTo(this);
        loader.setLayout(new BorderLayout());
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setForeground(UIStyles.LIGHT_TEXT);
        label.setFont(UIStyles.LABEL_FONT);
        loader.getContentPane().setBackground(UIStyles.DARK_BG);
        loader.add(label, BorderLayout.CENTER);
        new javax.swing.Timer(2000, e -> loader.dispose()).start();
        loader.setVisible(true);
    }

    public void scrollToBottom(JScrollPane scrollPane) {
        UIStyles.scrollToBottom(scrollPane);
    }

    public long generateMessageId() {
        return messageIdGenerator.getAndIncrement();
    }

    public SQLDatabase getDatabase() {
        return db;
    }

    public Map<String, UserData.User> getUsers() {
        return users;
    }

    public void setUsers(Map<String, UserData.User> users) {
        this.users = users;
    }

    public UserData.User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserData.User currentUser) {
        this.currentUser = currentUser;
    }

    public NetworkClient getNetworkClient() {
        return networkClient;
    }

    public void setNetworkClient(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    public CardLayout getCardLayout() {
        return cardLayout;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public ChatMainPanel getChatMainPanel() {
        return chatMainPanel;
    }

    public ProfilePanel getProfilePanel() {
        return profilePanel;
    }

    public String getSERVER_ADDRESS() {
        return SERVER_ADDRESS;
    }

    public int getSERVER_PORT() {
        return SERVER_PORT;
    }

    public Map<String, java.util.Set<String>> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, java.util.Set<String>> groups) {
        this.groups = groups;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChatClientFrame client = new ChatClientFrame();
            client.setVisible(true);
        });
    }
}