import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String messageId;
    private String sender;
    private String recipient;
    private String content;
    private String type;      // "MSG" for text or "FILE" for file messages
    private String fileData;  // Base64 encoded file data for file messages (null for text messages)
    private String status;    // "PENDING", "DELIVERED", or "READ"
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
    
    // Getters and Setters
    
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getFileData() { return fileData; }
    public void setFileData(String fileData) { this.fileData = fileData; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    @Override
    public String toString() {
        return sender + ": " + content;
    }
}
