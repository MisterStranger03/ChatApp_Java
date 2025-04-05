package Client;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserData {
    public static class User implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name, username, password, profilePhotoBase64;
        private Map<String, List<MessageData.Message>> chatHistory = new HashMap<>();
        private Map<String, Integer> unreadCounts = new HashMap<>();
        private Map<String, String> unreadSnippets = new HashMap<>();

        public User(String name, String username, String password) {
            this.name = name;
            this.username = username;
            this.password = password;
        }

        public String getName() {
            return name;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getProfilePhotoBase64() {
            return profilePhotoBase64;
        }

        public Map<String, List<MessageData.Message>> getChatHistory() {
            return chatHistory;
        }

        public Map<String, Integer> getUnreadCounts() {
            return unreadCounts;
        }

        public Map<String, String> getUnreadSnippets() {
            return unreadSnippets;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public void setProfilePhotoBase64(String profilePhotoBase64) {
            this.profilePhotoBase64 = profilePhotoBase64;
        }

        public void setChatHistory(Map<String, List<MessageData.Message>> chatHistory) {
            this.chatHistory = chatHistory;
        }

        public void setUnreadCounts(Map<String, Integer> unreadCounts) {
            this.unreadCounts = unreadCounts;
        }

        public void setUnreadSnippets(Map<String, String> unreadSnippets) {
            this.unreadSnippets = unreadSnippets;
        }
    }
}
