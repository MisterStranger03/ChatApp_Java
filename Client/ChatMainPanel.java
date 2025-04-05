package chatting;

import org.pushingpixels.trident.Timeline;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ChatMainPanel extends JPanel {

    public final ChatClientFrame client;
    public DefaultListModel<String> contactsModel;
    public JList<String> contactsList;
    public JPanel chatSessionPanel;
    public JLabel headerLabel;
    public String currentChatContact = null;
    public JPanel conversationPanel;

    public ChatMainPanel(ChatClientFrame client) {
        this.client = client;
        setLayout(new BorderLayout());
        setBackground(UIStyles.DARK_BG);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UIStyles.DARKER_BG);
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        headerLabel = new JLabel("Welcome, ...", SwingConstants.LEFT);
        headerLabel.setFont(UIStyles.TITLE_FONT);
        headerLabel.setForeground(UIStyles.ACCENT_COLOR);
        headerPanel.add(headerLabel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        JButton refreshChatButton = new JButton("REFRESH CHAT");
        client.styleButton(refreshChatButton);
        refreshChatButton.addActionListener(e -> {
            client.setUsers(client.getDatabase().loadUsers());
            refreshContacts();
            refreshChatHistory();
            JOptionPane.showMessageDialog(ChatMainPanel.this, "Chats Refreshed!");
        });
        rightPanel.add(refreshChatButton);

        JButton profileButton = new JButton("PROFILE");
        client.styleButton(profileButton);
        profileButton.addActionListener(e -> {
            if (client.getCurrentUser() != null) {
                client.getProfilePanel().loadProfileData();
                client.getCardLayout().show(client.getMainPanel(), "profile");
            }
        });
        rightPanel.add(profileButton);

        JButton logoutButton = new JButton("LOGOUT");
        client.styleButton(logoutButton);
        logoutButton.addActionListener(e -> {
            if (client.getNetworkClient() != null) {
                client.getNetworkClient().close();
                client.setNetworkClient(null);
            }
            client.setCurrentUser(null);
            client.getCardLayout().show(client.getMainPanel(), "login");
        });
        rightPanel.add(logoutButton);

        headerPanel.add(rightPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        contactsModel = new DefaultListModel<>();
        contactsList = new JList<>(contactsModel);
        contactsList.setBackground(UIStyles.DARKER_BG);
        contactsList.setForeground(UIStyles.LIGHT_TEXT);
        contactsList.setFont(UIStyles.FIELD_FONT);

        JScrollPane contactsScroll = new JScrollPane(contactsList);
        contactsScroll.setPreferredSize(new Dimension(250, 0));
        contactsScroll.setBorder(BorderFactory.createTitledBorder(new LineBorder(UIStyles.ACCENT_COLOR), "Contacts"));
        contactsScroll.getViewport().setBackground(UIStyles.DARKER_BG);

        JButton createGroupButton = new JButton("Create Group");
        client.styleButton(createGroupButton);
        createGroupButton.addActionListener(e -> openCreateGroupDialog());

        //friend---------------------------------------------
        JButton addFriendButton = new JButton("Add Friend");
        client.styleButton(addFriendButton);
        JButton removeFriendButton = new JButton("Remove Friend");
        client.styleButton(removeFriendButton);

        // client.styleButton();
        
        JPanel FriendButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        FriendButtons.setBackground(UIStyles.DARKER_BG);
        FriendButtons.add(addFriendButton);
        FriendButtons.add(removeFriendButton);

        addFriendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAddFriendDialog(client, client.getCurrentUser().getUsername());
            }
        });
        
        removeFriendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showRemoveFriendDialog();
            }
        });

        JPanel leftPanel = new JPanel(new BorderLayout());
        
        leftPanel.add(createGroupButton, BorderLayout.NORTH);
        leftPanel.add(contactsScroll, BorderLayout.CENTER);
        leftPanel.add(FriendButtons, BorderLayout.SOUTH);

        chatSessionPanel = new JPanel(new BorderLayout());
        chatSessionPanel.setBackground(UIStyles.DARKER_BG);
        chatSessionPanel.setBorder(BorderFactory.createTitledBorder(new LineBorder(UIStyles.ACCENT_COLOR), "Conversation"));

        contactsList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String display = contactsList.getSelectedValue();
                    System.out.println("Double clicked on: " + display);
                    if (display != null) {
                        if (display.startsWith("Group:")) {
                            String groupName = display.substring(6).trim();
                            System.out.println("Opening group chat for: " + groupName);
                            openGroupChatSession(groupName);
                        } else {
                            String contact = display.split(" ")[0];
                            System.out.println("Opening individual chat for: " + contact);
                            openIndividualChatSession(contact);
                        }
                    } else {
                        System.out.println("No contact selected.");
                    }
                }
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, chatSessionPanel);
        splitPane.setDividerLocation(250);
        add(splitPane, BorderLayout.CENTER);

        new javax.swing.Timer(5000, e -> {
            client.setUsers(client.getDatabase().loadUsers());
            refreshContacts();
            refreshChatHistory();
            client.initializeFriendManager();
        }).start();
    }

    public void showAddFriendDialog(ChatClientFrame client,String username) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Add Friend");
        dialog.setSize(300, 400);
        dialog.setLayout(new BorderLayout());
        
        JTextField searchField = new JTextField();
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(listModel);
        
        List<String> allUsers = new ArrayList<>(client.getDatabase().loadUsers().keySet());
        System.out.println("All users loaded: " + allUsers); // Debugging

        for (String user : allUsers) {
            if (!user.equals(username) && !client.isFriend(user)) {
                System.out.println("Adding to list: " + user); // Debugging
                listModel.addElement(user);
            }
        }

        
        searchField.addActionListener(e -> {
            String searchText = searchField.getText().toLowerCase();
            listModel.clear();
            for (String user : allUsers) {
                if (user.toLowerCase().contains(searchText) && !user.equals(username) && !client.isFriend(user)) {
                    listModel.addElement(user);
                }
            }
        });
        
        JButton addButton = new JButton("Add");
        client.styleButton(addButton);
        addButton.addActionListener(e -> {
            String selectedUser = userList.getSelectedValue();
            if (selectedUser != null) {
                System.out.println("Adding friend: " + selectedUser); // Debugging
                client.addFriend(selectedUser);
                JOptionPane.showMessageDialog(dialog, selectedUser + " added as friend.");
                refreshContacts();
                dialog.dispose();
            }
        });
        
        dialog.add(searchField, BorderLayout.NORTH);
        dialog.add(new JScrollPane(userList), BorderLayout.CENTER);
        dialog.add(addButton, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
    
    public void showRemoveFriendDialog() {
        JDialog dialog = new JDialog();
        dialog.setTitle("Remove Friend");
        dialog.setSize(300, 400);
        dialog.setLayout(new BorderLayout());
        
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> friendList = new JList<>(listModel);
        
        List<String> friends = client.getFriendsList();
        System.out.println("Friends loaded for removal: " + friends); // Debugging

        for (String friend : friends) {
            listModel.addElement(friend);
        }

        
        JButton removeButton = new JButton("Remove");
        client.styleButton(removeButton);
        removeButton.addActionListener(e -> {
            String selectedFriend = friendList.getSelectedValue();
            if (selectedFriend != null) {
                client.removeFriend(selectedFriend);
                JOptionPane.showMessageDialog(dialog, selectedFriend + " removed from friends.");
                refreshContacts();
                dialog.dispose();
            }
        });
        
        dialog.add(new JScrollPane(friendList), BorderLayout.CENTER);
        dialog.add(removeButton, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        
    }

    public void refreshContacts() {
        UserData.User currentUser = client.getCurrentUser();
        if (currentUser == null) return;
        headerLabel.setText("Welcome, " + currentUser.getName());
        contactsModel.clear();
        for (Map.Entry<String, UserData.User> entry : client.getUsers().entrySet()) {
            String uname = entry.getKey();
            if (!uname.equals(currentUser.getUsername()) && client.isFriend(uname)) {
                int unread = currentUser.getUnreadCounts().getOrDefault(uname, 0);
                String snippet = currentUser.getUnreadSnippets().getOrDefault(uname, "");
                String display = uname;
                if (unread > 0) {
                    display += " (" + unread + ")";
                    if (!snippet.isEmpty()) {
                        display += " - " + snippet;
                    }
                }
                contactsModel.addElement(display);
            }
        }
        for (String groupName : client.getGroups().keySet()) {
            contactsModel.addElement("Group: " + groupName);
        }
    }

    public void refreshChatHistory() {
        // Optionally update conversation panel if needed.
    }

    public JScrollPane createInputAreaPanel(JPanel parentPanel, Consumer<String> sendAction) {
        JTextArea inputArea = new JTextArea(1, 30);
        client.styleInputArea(inputArea);
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
        UserData.User currentUser  = client.getCurrentUser ();
        currentUser.getUnreadCounts().put(contact, 0);
        currentUser.getUnreadSnippets().remove(contact);
        refreshContacts();
    
        chatSessionPanel.removeAll();
        chatSessionPanel.setLayout(new BorderLayout());
    
        conversationPanel = new JPanel();
        conversationPanel.setLayout(new BoxLayout(conversationPanel, BoxLayout.Y_AXIS));
        conversationPanel.setBackground(UIStyles.DARK_BG);
    
        final JScrollPane convScroll = new JScrollPane(conversationPanel);
        convScroll.setBorder(null);
        convScroll.getViewport().setBackground(UIStyles.DARK_BG);
    
        // Remove the incorrect caret handling
        // DefaultCaret caret = (DefaultCaret) convScroll.getVerticalScrollBar().getModel();
        // caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    
        List<MessageData.Message> history = currentUser.getChatHistory().get(contact);
        if (history != null) {
            Collections.sort(history, Comparator.comparingLong(MessageData.Message::getTimestamp));
            String lastDate = null;
            SimpleDateFormat sdfDate = new SimpleDateFormat("MMM dd, yyyy");
            for (MessageData.Message m : history) {
                String msgDate = sdfDate.format(new Date(m.getTimestamp()));
                if (lastDate == null || !lastDate.equals(msgDate)) {
                    conversationPanel.add(new DateHeader(msgDate));
                    lastDate = msgDate;
                }
                conversationPanel.add(createMessagePanel(m));
            }
        }
        client.scrollToBottom(convScroll);
    
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        inputPanel.setBackground(UIStyles.DARKER_BG);
    
        final JScrollPane inputScroll = createInputAreaPanel(inputPanel, msgText -> {
            if (!msgText.isEmpty()) {
                String msgId = currentUser .getUsername() + "-" + client.generateMessageId();
                MessageData.Message msg = new MessageData.Message(msgId, currentUser .getUsername(), contact, msgText, "MSG", null);
                msg.setStatus("PENDING");
                addMessageToHistory(contact, msg);
                SimpleDateFormat sdfDate = new SimpleDateFormat("MMM dd, yyyy");
                String msgDate = sdfDate.format(new Date(msg.getTimestamp()));
                Component last = conversationPanel.getComponentCount() > 0 ? conversationPanel.getComponent(conversationPanel.getComponentCount() - 1) : null;
                String lastHeader = (last instanceof DateHeader) ? ((DateHeader) last).getDateText() : null;
                if (lastHeader == null || !lastHeader.equals(msgDate)) {
                    conversationPanel.add(new DateHeader(msgDate));
                }
                conversationPanel.add(createMessagePanel(msg));
                conversationPanel.revalidate();
                conversationPanel.repaint();
                NetworkClient networkClient = client.getNetworkClient();
                if (networkClient != null) {
                    networkClient.sendMessage(contact, "MSG|" + msgId + "|" + currentUser .getUsername() + "|" + contact + "|" + msgText);
                }
                client.scrollToBottom(convScroll);
            }
        });
    
        JButton sendMsgButton = new JButton("Send");
        client.styleButton(sendMsgButton);
    
        JButton sendFileButton = new JButton("Send File");
        client.styleButton(sendFileButton);
    
        inputPanel.add(inputScroll);
        inputPanel.add(sendMsgButton);
        inputPanel.add(sendFileButton);
    
        JButton closeChatButton = new JButton("Close Chat");
        client.styleButton(closeChatButton);
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
                    String msgId = currentUser .getUsername() + "-" + client.generateMessageId();
                    MessageData.Message fileMsg = new MessageData.Message(msgId, currentUser .getUsername(), contact, file.getName(), "FILE", base64Encoded);
                    fileMsg.setStatus("PENDING");
                    addMessageToHistory(contact, fileMsg);
                    conversationPanel.add(createMessagePanel(fileMsg));
                    conversationPanel.revalidate();
                    conversationPanel.repaint();
                    NetworkClient networkClient = client.getNetworkClient();
                    if (networkClient != null) {
                        networkClient.sendMessage(contact, "FILE|" + msgId + "|" + currentUser .getUsername() + "|" + contact + "|" + file.getName() + "|" + base64Encoded);
                    }
                    client.scrollToBottom(convScroll);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(chatSessionPanel, "Error reading file: " + ex.getMessage());
                }
            }
        });
    
        if (currentUser .getChatHistory().containsKey(contact)) {
            for (MessageData.Message m : currentUser .getChatHistory().get(contact)) {
                if (!"READ".equals(m.getStatus()) && m.getSender().equals(contact)) {
                    m.setStatus("READ");
                    NetworkClient networkClient = client.getNetworkClient();
                    if (networkClient != null) {
                        networkClient.sendMessage(contact, "ACK|" + m.getMessageId() + "|READ");
                    }
                }
            }
            client.getDatabase().saveMessagesForUser (currentUser .getUsername(), currentUser .getChatHistory());
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
        groupHeader.setBackground(UIStyles.DARKER_BG);
        JLabel groupLabel = new JLabel("Group: " + groupName);
        groupLabel.setFont(UIStyles.TITLE_FONT);
        groupLabel.setForeground(UIStyles.ACCENT_COLOR);
        groupHeader.add(groupLabel, BorderLayout.WEST);
    
        groupLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                MouseEvent me = (MouseEvent) e;
                JPopupMenu menu = new JPopupMenu();
    
                JMenuItem leaveItem = new JMenuItem("Leave Group");
                leaveItem.addActionListener(ae -> {
                    NetworkClient networkClient = client.getNetworkClient();
                    if (networkClient != null) {
                        networkClient.sendMessage("", "LEAVE_GROUP|" + groupName + "|" + client.getCurrentUser ().getUsername());
                        client.getGroups().remove(groupName);
                        JOptionPane.showMessageDialog(ChatMainPanel.this, "You have left the group.");
                        chatSessionPanel.removeAll();
                        chatSessionPanel.revalidate();
                        chatSessionPanel.repaint();
                        currentChatContact = null;
                        refreshContacts();
                    }
                });
                menu.add(leaveItem);
    
                JMenuItem changeNameItem = new JMenuItem("Change Group Name");
                changeNameItem.addActionListener(ae -> {
                    String newName = JOptionPane.showInputDialog(ChatMainPanel.this, "Enter new group name:");
                    if (newName != null && !newName.trim().isEmpty()) {
                        NetworkClient networkClient = client.getNetworkClient();
                        if (networkClient != null) {
                            networkClient.sendMessage("", "UPDATE_GROUP|" + groupName + "|" + newName + "|" + client.getCurrentUser ().getUsername());
                            java.util.Set<String> members = client.getGroups().get(groupName);
                            client.getGroups().remove(groupName);
                            client.getGroups().put(newName, members);
                            groupLabel.setText("Group: " + newName);
                            refreshContacts();
                        }
                    }
                });
                menu.add(changeNameItem);
    
                JMenuItem showMembersItem = new JMenuItem("Show Members");
                showMembersItem.addActionListener(ae -> {
                    NetworkClient networkClient = client.getNetworkClient();
                    if (networkClient != null) {
                        networkClient.sendMessage("", "GROUP_INFO|" + groupName);
                    }
                    SwingUtilities.invokeLater(() -> {
                        java.util.Set<String> mem = client.getGroups().get(groupName);
                        if (mem != null) {
                            JOptionPane.showMessageDialog(ChatMainPanel.this, "Members: " + String.join(", ", mem));
                        } else {
                            JOptionPane.showMessageDialog(ChatMainPanel.this, "No member info available.");
                        }
                    });
                });
                menu.add(showMembersItem);
    
                JMenuItem addUserItem = new JMenuItem("Add User");
                addUserItem.addActionListener(ae -> {
                    java.util.Set<String> currentMembers = client.getGroups().get(groupName);
                    List<String> availableUsers = client.getUsers().keySet().stream()
                            .filter(uname -> !uname.equals(client.getCurrentUser ().getUsername()) && (currentMembers == null || !currentMembers.contains(uname)))
                            .collect(Collectors.toList());
    
                    if (availableUsers.isEmpty()) {
                        JOptionPane.showMessageDialog(ChatMainPanel.this, "No users available to add.");
                        return;
                    }
    
                    JPanel panel = new JPanel();
                    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                    Map<String, JCheckBox> checkboxMap = new HashMap<>();
                    for (String user : availableUsers) {
                        JCheckBox cb = new JCheckBox(user);
                        cb.setForeground(UIStyles.LIGHT_TEXT);
                        cb.setBackground(UIStyles.DARK_BG);
                        panel.add(cb);
                        checkboxMap.put(user, cb);
                    }
    
                    int result = JOptionPane.showConfirmDialog(ChatMainPanel.this, panel, "Select users to add", JOptionPane.OK_CANCEL_OPTION);
                    if (result == JOptionPane.OK_OPTION) {
                        NetworkClient networkClient = client.getNetworkClient();
                        if (networkClient != null) {
                            for (Map.Entry<String, JCheckBox> entry : checkboxMap.entrySet()) {
                                if (entry.getValue().isSelected()) {
                                    networkClient.sendMessage("", "ADD_TO_GROUP|" + groupName + "|" + client.getCurrentUser ().getUsername() + "|" + entry.getKey());
                                }
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
        conversationPanel.setBackground(UIStyles.DARK_BG);
    
        final JScrollPane convScroll = new JScrollPane(conversationPanel);
        convScroll.setBorder(null);
        convScroll.getViewport().setBackground(UIStyles.DARK_BG);
    
        // Remove the incorrect caret handling
        // DefaultCaret caret = (DefaultCaret) convScroll.getVerticalScrollBar().getModel();
        // caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    
        List<MessageData.Message> history = client.getCurrentUser ().getChatHistory().get("Group:" + groupName);
        if (history != null) {
            Collections.sort(history, Comparator.comparingLong(MessageData.Message::getTimestamp));
            String lastDate = null;
            SimpleDateFormat sdfDate = new SimpleDateFormat("MMM dd, yyyy");
            for (MessageData.Message m : history) {
                String msgDate = sdfDate.format(new Date(m.getTimestamp()));
                if (lastDate == null || !lastDate.equals(msgDate)) {
                    conversationPanel.add(new DateHeader(msgDate));
                    lastDate = msgDate;
                }
                conversationPanel.add(createMessagePanel(m));
            }
        }
        client.scrollToBottom(convScroll);
    
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        inputPanel.setBackground(UIStyles.DARK_BG);
    
        final JScrollPane inputScroll = createInputAreaPanel(inputPanel, msgText -> {
            if (!msgText.isEmpty()) {
                String msgId = client.getCurrentUser ().getUsername() + "-" + client.generateMessageId();
                MessageData.Message msg = new MessageData.Message(msgId, client.getCurrentUser ().getUsername(), groupName, msgText, "GROUP_MSG", null);
                msg.setStatus("PENDING");
                addMessageToHistory("Group:" + groupName, msg);
                SimpleDateFormat sdfDate = new SimpleDateFormat("MMM dd, yyyy");
                String msgDate = sdfDate.format(new Date(msg.getTimestamp()));
                Component last = conversationPanel.getComponentCount() > 0 ? conversationPanel.getComponent(conversationPanel.getComponentCount() - 1) : null;
                String lastHeader = (last instanceof DateHeader) ? ((DateHeader) last).getDateText() : null;
                if (lastHeader == null || !lastHeader.equals(msgDate)) {
                    conversationPanel.add(new DateHeader(msgDate));
                }
                conversationPanel.add(createMessagePanel(msg));
                conversationPanel.revalidate();
                conversationPanel.repaint();
                NetworkClient networkClient = client.getNetworkClient();
                if (networkClient != null) {
                    networkClient.sendMessage("", "GROUP_MSG|" + msgId + "|" + client.getCurrentUser ().getUsername() + "|" + groupName + "|" + msgText);
                }
                client.scrollToBottom(convScroll);
            }
        });
    
        JButton sendMsgButton = new JButton("Send");
        client.styleButton(sendMsgButton);
    
        JButton sendFileButton = new JButton("Send File");
        client.styleButton(sendFileButton);
    
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
                    String msgId = client.getCurrentUser ().getUsername() + "-" + client.generateMessageId();
                    MessageData.Message fileMsg = new MessageData.Message(msgId, client.getCurrentUser ().getUsername(), groupName, file.getName(), "GROUP_FILE", base64Encoded);
                    fileMsg.setStatus("PENDING");
                    addMessageToHistory("Group:" + groupName, fileMsg);
                    conversationPanel.add(createMessagePanel(fileMsg));
                    conversationPanel.revalidate();
                    conversationPanel.repaint();
                    NetworkClient networkClient = client.getNetworkClient();
                    if (networkClient != null) {
                        networkClient.sendMessage("", "GROUP_FILE|" + msgId + "|" + client.getCurrentUser ().getUsername() + "|" + groupName + "|" + file.getName() + "|" + base64Encoded);
                    }
                    client.scrollToBottom(convScroll);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(chatSessionPanel, "Error reading file: " + ex.getMessage());
                }
            }
        });
    
        JButton closeChatButton = new JButton("Close Chat");
        client.styleButton(closeChatButton);
        closeChatButton.addActionListener(e -> {
            chatSessionPanel.removeAll();
            chatSessionPanel.revalidate();
            chatSessionPanel.repaint();
            currentChatContact = null;
        });
    
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        bottomPanel.setBackground(UIStyles.DARK_BG);
        bottomPanel.add(inputScroll);
        bottomPanel.add(sendMsgButton);
        bottomPanel.add(sendFileButton);
        bottomPanel.add(closeChatButton);
    
        chatSessionPanel.add(convScroll, BorderLayout.CENTER);
        chatSessionPanel.add(bottomPanel, BorderLayout.SOUTH);
        chatSessionPanel.revalidate();
        chatSessionPanel.repaint();
    }

    public void openCreateGroupDialog() {
        JDialog groupDialog = new JDialog(client, "Create Group", true);
        groupDialog.setSize(400, 400);
        groupDialog.setLocationRelativeTo(client);
        groupDialog.setLayout(new BorderLayout());

        JPanel selectionPanel = new JPanel();
        selectionPanel.setLayout(new BoxLayout(selectionPanel, BoxLayout.Y_AXIS));
        Map<String, JCheckBox> checkBoxes = new HashMap<>();
        for (Map.Entry<String, UserData.User> entry : client.getUsers().entrySet()) {
            String uname = entry.getKey();
            if (!uname.equals(client.getCurrentUser().getUsername())) {
                JCheckBox cb = new JCheckBox(uname);
                cb.setForeground(UIStyles.LIGHT_TEXT);
                cb.setBackground(UIStyles.DARK_BG);
                checkBoxes.put(uname, cb);
                selectionPanel.add(cb);
            }
        }
        JScrollPane scrollPane = new JScrollPane(selectionPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Select Contacts"));
        scrollPane.getViewport().setBackground(UIStyles.DARK_BG);

        JPanel groupNamePanel = new JPanel(new FlowLayout());
        groupNamePanel.setBackground(UIStyles.DARK_BG);
        JLabel nameLabel = new JLabel("Group Name:");
        nameLabel.setForeground(UIStyles.LIGHT_TEXT);
        JTextField groupNameField = new JTextField(20);
        client.styleTextField(groupNameField);
        groupNamePanel.add(nameLabel);
        groupNamePanel.add(groupNameField);

        JButton createBtn = new JButton("Create");
        client.styleButton(createBtn);
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
            selected.add(client.getCurrentUser().getUsername());
            String membersStr = String.join(",", selected);
            NetworkClient networkClient = client.getNetworkClient();
            if (networkClient != null) {
                networkClient.sendMessage("", "CREATE_GROUP|" + groupName + "|" + client.getCurrentUser().getUsername() + "|" + membersStr);
            }
            client.getGroups().put(groupName, new HashSet<>(selected));
            refreshContacts();
            groupDialog.dispose();
        });

        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.setBackground(UIStyles.DARK_BG);
        bottomPanel.add(createBtn);

        groupDialog.add(scrollPane, BorderLayout.CENTER);
        groupDialog.add(groupNamePanel, BorderLayout.NORTH);
        groupDialog.add(bottomPanel, BorderLayout.SOUTH);
        groupDialog.setVisible(true);
    }

    public JPanel createMessagePanel(MessageData.Message m) {
        boolean isSelf = m.getSender().equals(client.getCurrentUser().getUsername());
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
        slideTimeline.addPropertyToInterpolate("offset", bubble.getOffset(), 0);
        slideTimeline.setDuration(500);
        slideTimeline.play();

        if (m.getType().equals("FILE") || m.getType().equals("GROUP_FILE")) {
            JButton downloadBtn = new JButton("Download");
            client.styleButton(downloadBtn);
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

    public void addMessageToHistory(String contact, MessageData.Message m) {
        client.getCurrentUser().getChatHistory().computeIfAbsent(contact, k -> new ArrayList<>()).add(m);
        if (currentChatContact == null || !currentChatContact.equals(contact)) {
            int cnt = client.getCurrentUser().getUnreadCounts().getOrDefault(contact, 0) + 1;
            client.getCurrentUser().getUnreadCounts().put(contact, cnt);
            String snippet = (m.getType().equals("FILE") || m.getType().equals("GROUP_FILE"))
                    ? "[File: " + m.getContent() + "]" : m.getContent();
            if (snippet.length() > 20) {
                snippet = snippet.substring(0, 20) + "...";
            }
            client.getCurrentUser().getUnreadSnippets().put(contact, snippet);
        }
        client.getDatabase().saveMessage(m);
    }

    public void updateConversation(String contact, String displayText) {
        if (currentChatContact != null && currentChatContact.equals(contact)) {
            JLabel label = new JLabel(displayText);
            label.setForeground(UIStyles.LIGHT_TEXT);
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(UIStyles.DARK_BG);
            p.setBorder(new EmptyBorder(5, 5, 5, 5));
            p.add(label, BorderLayout.CENTER);
            conversationPanel.add(p);
            conversationPanel.revalidate();
            conversationPanel.repaint();
        }
    }

    public static class RoundedBorder extends AbstractBorder {
        public int radius;

        public RoundedBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.getBackground());
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius + 1, radius + 1, radius + 1, radius + 1);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.top = insets.right = insets.bottom = radius + 1;
            return insets;
        }
    }

    public static class ChatBubble extends JPanel {
        public String message;
        public String timeText;
        public Color bubbleColor;
        public float alpha = 0f;
        public int offset;

        public ChatBubble(String message, long timestamp, Color bubbleColor, boolean isSelf) {
            this.message = message;
            this.bubbleColor = bubbleColor;
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
            this.timeText = sdf.format(new Date(timestamp));
            this.offset = isSelf ? 50 : -50;
            setOpaque(false);
            setBorder(new EmptyBorder(8, 12, 20, 12));
        }

        public float getAlpha() {
            return alpha;
        }

        public void setAlpha(float alpha) {
            this.alpha = alpha;
            repaint();
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            Graphics g = getGraphics();
            if (g == null) {
                g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).getGraphics();
            }
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

    public static class DateHeader extends JPanel {
        public String dateText;

        public DateHeader(String dateText) {
            this.dateText = dateText;
            setOpaque(false);
            setPreferredSize(new Dimension(300, 30));
        }

        public String getDateText() {
            return dateText;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = 15;
            int width = getWidth();
            int height = getHeight();
            g2.setColor(UIStyles.DATE_BG);
            g2.fillRoundRect(0, 0, width, height, arc, arc);
            g2.setColor(UIStyles.LIGHT_TEXT);
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
}