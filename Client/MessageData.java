package chatting;

import java.io.Serializable;

public class MessageData {
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

        public String getMessageId() {
            return messageId;
        }

        public String getSender() {
            return sender;
        }

        public String getRecipient() {
            return recipient;
        }

        public String getContent() {
            return content;
        }

        public String getType() {
            return type;
        }

        public String getFileData() {
            return fileData;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
