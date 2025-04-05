package chatting;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class ProfilePanel extends JPanel {

    public final ChatClientFrame client;
    public JLabel photoLabel;
    public JTextField nameField, usernameField;
    public JPasswordField passwordField;
    public JButton saveButton, loadPhotoButton;

    public ProfilePanel(ChatClientFrame client) {
        this.client = client;
        setLayout(new BorderLayout());
        setBackground(UIStyles.DARK_BG);

        JLabel title = new JLabel("Profile Settings", SwingConstants.CENTER);
        title.setFont(UIStyles.TITLE_FONT);
        title.setForeground(new Color(255, 200, 100));
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
        JLabel photoTextLabel = new JLabel("Profile Photo:");
        photoTextLabel.setFont(UIStyles.LABEL_FONT);
        photoTextLabel.setForeground(UIStyles.LIGHT_TEXT);
        formPanel.add(photoTextLabel, gbc);

        gbc.gridx = 1;
        photoLabel = new JLabel();
        photoLabel.setPreferredSize(new Dimension(100, 100));
        photoLabel.setOpaque(true);
        photoLabel.setBackground(UIStyles.FIELD_BG);
        formPanel.add(photoLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        loadPhotoButton = new JButton("Load Photo");
        client.styleButton(loadPhotoButton);
        formPanel.add(loadPhotoButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        saveButton = new JButton("SAVE CHANGES");
        client.styleButton(saveButton);
        formPanel.add(saveButton, gbc);

        add(formPanel, BorderLayout.CENTER);

        loadPhotoButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif"));
            int res = fc.showOpenDialog(ProfilePanel.this);
            if (res == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                try {
                    byte[] data = Files.readAllBytes(f.toPath());
                    String base64 = Base64.getEncoder().encodeToString(data);
                    client.getCurrentUser().setProfilePhotoBase64(base64);
                    ImageIcon icon = new ImageIcon(Base64.getDecoder().decode(base64));
                    Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                    photoLabel.setIcon(new ImageIcon(img));
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(ProfilePanel.this, "Error loading photo: " + ex.getMessage());
                }
            }
        });

        saveButton.addActionListener(e -> {
            if (client.getCurrentUser() == null) return;
            client.showLoader("Saving Profile...");
            client.getCurrentUser().setName(nameField.getText().trim());
            client.getCurrentUser().setUsername(usernameField.getText().trim());
            client.getCurrentUser().setPassword(new String(passwordField.getPassword()));
            client.getDatabase().updateUser(client.getCurrentUser());
            JOptionPane.showMessageDialog(ProfilePanel.this, "Profile updated!");
            client.getChatMainPanel().refreshContacts();
            client.getCardLayout().show(client.getMainPanel(), "chat");
        });
    }

    public void loadProfileData() {
        UserData.User currentUser = client.getCurrentUser();
        if (currentUser != null) {
            nameField.setText(currentUser.getName());
            usernameField.setText(currentUser.getUsername());
            passwordField.setText(currentUser.getPassword());
            if (currentUser.getProfilePhotoBase64() != null) {
                ImageIcon icon = new ImageIcon(Base64.getDecoder().decode(currentUser.getProfilePhotoBase64()));
                Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                photoLabel.setIcon(new ImageIcon(img));
            } else {
                photoLabel.setIcon(null);
                photoLabel.setText("No Photo");
            }
        }
    }
}