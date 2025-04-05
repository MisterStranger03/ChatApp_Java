package chatting;

import java.util.List;

public class Friend {
    private String username;
    private SQLDatabase database;

    
    public Friend(String username, SQLDatabase database) {
        this.username = username;
        this.database = database;
        
    }
    
    public boolean addFriend(String friendUsername) {
        return database.addFriend(username, friendUsername); // return database success status
    }
    
    public List<String> getFriends() {
        return database.getFriends(username);
    }
    
    public boolean isFriend(String friendUsername) {
        return database.isFriend(username, friendUsername);
    }
    
    public boolean removeFriend(String friendUsername) {
        return database.removeFriend(username, friendUsername); // return database success status
    }

}