package chatting;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class RegistrationPanel extends JPanel {

    public final ChatClientFrame client;
    public JTextField nameField;
    public JTextField usernameField;
    public JPasswordField passwordField;

    public RegistrationPanel(ChatClientFrame client) {
        this.client = client;
        setBackground(UIStyles.DARK_BG);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Create Account", SwingConstants.CENTER);
        title.setFont(UIStyles.TITLE_FONT);
        title.setForeground(new Color(200, 100, 255));
        add(title, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UIStyles.DARK_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(UIStyles.LABEL_FONT);
        nameLabel.setForeground(UIStyles.LIGHT_TEXT);
        formPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        nameField = new JTextField(20);
        client.styleTextField(nameField);
        formPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(UIStyles.LABEL_FONT);
        userLabel.setForeground(UIStyles.LIGHT_TEXT);
        formPanel.add(userLabel, gbc);

        gbc.gridx = 1;
        usernameField = new JTextField(20);
        client.styleTextField(usernameField);
        formPanel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(UIStyles.LABEL_FONT);
        passLabel.setForeground(UIStyles.LIGHT_TEXT);
        formPanel.add(passLabel, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        client.styleTextField(passwordField);
        formPanel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JButton registerButton = new JButton("REGISTER");
        client.styleButton(registerButton);
        formPanel.add(registerButton, gbc);

        gbc.gridy = 4;
        JButton backToLoginButton = new JButton("BACK TO LOGIN");
        client.styleButton(backToLoginButton);
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

            Map<String, UserData.User> users = client.getUsers();
            if (users.containsKey(username)) {
                JOptionPane.showMessageDialog(this, "Username already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            UserData.User newUser = new UserData.User(name, username, password);
            users.put(username, newUser);
            client.getDatabase().saveUser(newUser);
            client.setUsers(users);
            client.showLoader("Registering...");
            JOptionPane.showMessageDialog(this, "Registration successful! Please login.", "Success", JOptionPane.INFORMATION_MESSAGE);
            client.getCardLayout().show(client.getMainPanel(), "login");
        });

        backToLoginButton.addActionListener(e -> {
            client.showLoader("Going back to Login...");
            client.getCardLayout().show(client.getMainPanel(), "login");
        });
    }
}