package chatting;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class LoginPanel extends JPanel {

    public final ChatClientFrame client;
    public JTextField usernameField;
    public JPasswordField passwordField;

    public LoginPanel(ChatClientFrame client) {
        this.client = client;
        setBackground(UIStyles.DARK_BG);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Welcome Back", SwingConstants.CENTER);
        title.setFont(UIStyles.TITLE_FONT);
        title.setForeground(UIStyles.ACCENT_COLOR);
        add(title, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UIStyles.DARK_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(UIStyles.LABEL_FONT);
        userLabel.setForeground(UIStyles.LIGHT_TEXT);
        formPanel.add(userLabel, gbc);

        gbc.gridx = 1;
        usernameField = new JTextField(20);
        client.styleTextField(usernameField);
        formPanel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(UIStyles.LABEL_FONT);
        passLabel.setForeground(UIStyles.LIGHT_TEXT);
        formPanel.add(passLabel, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        client.styleTextField(passwordField);
        formPanel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        JButton loginButton = new JButton("LOGIN");
        client.styleButton(loginButton);
        formPanel.add(loginButton, gbc);

        gbc.gridy = 3;
        JButton toRegisterButton = new JButton("REGISTER");
        client.styleButton(toRegisterButton);
        formPanel.add(toRegisterButton, gbc);

        add(formPanel, BorderLayout.CENTER);

        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Map<String, UserData.User> users = client.getUsers();
            if (users.containsKey(username)) {
                UserData.User user = users.get(username);
                if (user.getPassword().equals(password)) {
                    client.showLoader("Logging in...");
                    client.setCurrentUser(user);
                    // Load individual and group messages
                    user.setChatHistory(client.getDatabase().loadMessagesForUser(client.getCurrentUser().getUsername()));
                    client.getChatMainPanel().refreshContacts();
                    client.getChatMainPanel().refreshChatHistory();
                    NetworkClient networkClient = new NetworkClient(client.getCurrentUser().getUsername(), client);
                    client.setNetworkClient(networkClient);
                    new Thread(networkClient).start();
                    client.getCardLayout().show(client.getMainPanel(), "chat");
                } else {
                    JOptionPane.showMessageDialog(this, "Incorrect password.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "User not found.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        toRegisterButton.addActionListener(e -> {
            client.showLoader("Opening Registration...");
            client.getCardLayout().show(client.getMainPanel(), "register");
        });
    }
}