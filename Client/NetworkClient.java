package chatting;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;

public class NetworkClient implements Runnable {

    public Socket socket;
    public PrintWriter out;
    public BufferedReader in;
    public String username;
    public final ChatClientFrame client;

    public NetworkClient(String username, ChatClientFrame client) {
        this.username = username;
        this.client = client;
        try {
            socket = new Socket(client.getSERVER_ADDRESS(), client.getSERVER_PORT());
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println(username);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(client, "Unable to connect to server: " + e.getMessage());
        }
    }

    public void sendMessage(String recipient, String message) {
        if (out != null) {
            out.println(message);
        }
    }

    @Override
    public void run() {
        String line;
        try {
            while ((line = in.readLine()) != null) {
                String[] parts = line.split("\\|", 7);
                if (parts.length < 1) continue;
                String type = parts[0];
                ChatMainPanel chatMainPanel = client.getChatMainPanel();
                UserData.User currentUser = client.getCurrentUser();

                if (type.equals("MSG")) {
                    if (parts.length < 5) continue;
                    String msgId = parts[1];
                    String sender = parts[2];
                    String recipient = parts[3];
                    String content = parts[4];
                    MessageData.Message m = new MessageData.Message(msgId, sender, recipient, content, "MSG", null);
                    m.setStatus("DELIVERED");
                    if (currentUser != null && !sender.equals(currentUser.getUsername())) {
                        if (chatMainPanel.currentChatContact == null || !chatMainPanel.currentChatContact.equals(sender)) {
                            int cnt = currentUser.getUnreadCounts().getOrDefault(sender, 0) + 1;
                            currentUser.getUnreadCounts().put(sender, cnt);
                            String snippet = content.length() > 20 ? content.substring(0, 20) + "..." : content;
                            currentUser.getUnreadSnippets().put(sender, snippet);
                            SwingUtilities.invokeLater(chatMainPanel::refreshContacts);
                        }
                    }
                    currentUser.getChatHistory().computeIfAbsent(sender, k -> new java.util.ArrayList<>()).add(m);
                    SwingUtilities.invokeLater(() -> chatMainPanel.updateConversation(sender, sender + ": " + content + " ✔"));
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
                    MessageData.Message m = new MessageData.Message(msgId, sender, recipient, filename, "FILE", base64data);
                    m.setStatus("DELIVERED");
                    if (currentUser != null && !sender.equals(currentUser.getUsername())) {
                        if (chatMainPanel.currentChatContact == null || !chatMainPanel.currentChatContact.equals(sender)) {
                            int cnt = currentUser.getUnreadCounts().getOrDefault(sender, 0) + 1;
                            currentUser.getUnreadCounts().put(sender, cnt);
                            currentUser.getUnreadSnippets().put(sender, "[File] " + filename);
                            SwingUtilities.invokeLater(chatMainPanel::refreshContacts);
                        }
                    }
                    currentUser.getChatHistory().computeIfAbsent(sender, k -> new java.util.ArrayList<>()).add(m);
                    SwingUtilities.invokeLater(() -> chatMainPanel.updateConversation(sender, sender + " sent a file: " + filename + " ✔"));
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
                    MessageData.Message m = new MessageData.Message(msgId, sender, groupName, content, "GROUP_MSG", null);
                    m.setStatus("DELIVERED");
                    String localGroupKey = "Group:" + groupName;
                    if (currentUser != null && (chatMainPanel.currentChatContact == null || !chatMainPanel.currentChatContact.equals(localGroupKey))) {
                        int cnt = currentUser.getUnreadCounts().getOrDefault(localGroupKey, 0) + 1;
                        currentUser.getUnreadCounts().put(localGroupKey, cnt);
                        currentUser.getUnreadSnippets().put(localGroupKey, content.length() > 20 ? content.substring(0, 20) + "..." : content);
                        SwingUtilities.invokeLater(chatMainPanel::refreshContacts);
                    }
                    currentUser.getChatHistory().computeIfAbsent(localGroupKey, k -> new java.util.ArrayList<>()).add(m);
                    SwingUtilities.invokeLater(() -> chatMainPanel.updateConversation(localGroupKey, sender + " (in " + groupName + "): " + content + " ✔"));
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
                    MessageData.Message m = new MessageData.Message(msgId, sender, groupName, filename, "GROUP_FILE", base64data);
                    m.setStatus("DELIVERED");
                    String localGroupKey = "Group:" + groupName;
                    if (currentUser != null && (chatMainPanel.currentChatContact == null || !chatMainPanel.currentChatContact.equals(localGroupKey))) {
                        int cnt = currentUser.getUnreadCounts().getOrDefault(localGroupKey, 0) + 1;
                        currentUser.getUnreadCounts().put(localGroupKey, cnt);
                        currentUser.getUnreadSnippets().put(localGroupKey, filename);
                        SwingUtilities.invokeLater(chatMainPanel::refreshContacts);
                    }
                    currentUser.getChatHistory().computeIfAbsent(localGroupKey, k -> new java.util.ArrayList<>()).add(m);
                    SwingUtilities.invokeLater(() -> chatMainPanel.updateConversation(localGroupKey, sender + " (in " + groupName + ") sent a file: " + filename + " ✔"));
                    if (chatMainPanel.currentChatContact != null && chatMainPanel.currentChatContact.equals(localGroupKey)) {
                        sendMessage("", "ACK|" + msgId + "|READ");
                        m.setStatus("READ");
                    }
                } else if (type.equals("GROUP_CREATED")) {
                    if (parts.length >= 3) {
                        String groupName = parts[1];
                        String membersStr = parts[2];
                        java.util.Set<String> memSet = new HashSet<>(Arrays.asList(membersStr.split(",")));
                        client.getGroups().put(groupName, memSet);
                        SwingUtilities.invokeLater(chatMainPanel::refreshContacts);
                    }
                } else if (type.equals("GROUP_UPDATE")) {
                    if (parts.length >= 4) {
                        String groupName = parts[1];
                        String updateType = parts[2];
                        String data = parts[3];
                        java.util.Map<String, java.util.Set<String>> groups = client.getGroups();
                        if (updateType.equals("NAME_CHANGED")) {
                            java.util.Set<String> mem = groups.get(groupName);
                            groups.remove(groupName);
                            groups.put(data, mem);
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(client, "Group " + groupName + " renamed to " + data);
                                chatMainPanel.refreshContacts();
                            });
                        } else if (updateType.equals("MEMBER_LEFT")) {
                            java.util.Set<String> mem = groups.get(groupName);
                            if (mem != null) {
                                mem.remove(data);
                            }
                            SwingUtilities.invokeLater(chatMainPanel::refreshContacts);
                        } else if (updateType.equals("USER_ADDED")) {
                            java.util.Set<String> mem = groups.get(groupName);
                            if (mem != null) {
                                mem.add(data);
                            }
                            SwingUtilities.invokeLater(chatMainPanel::refreshContacts);
                        }
                        client.setGroups(groups);
                    }
                } else if (type.equals("ACK")) {
                    if (parts.length < 3) continue;
                    String msgId = parts[1];
                    String status = parts[2];
                    SwingUtilities.invokeLater(() -> System.out.println("Message " + msgId + " status updated: " + status));
                } else if (type.equals("GROUP_INFO")) {
                    if (parts.length >= 3) {
                        String groupName = parts[1];
                        String memStr = parts[2];
                        java.util.Set<String> memSet = new HashSet<>(Arrays.asList(memStr.split(",")));
                        client.getGroups().put(groupName, memSet);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
