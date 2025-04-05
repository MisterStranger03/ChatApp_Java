# Dark Mode Chat and File Sharing Application

A modern Java Swing-based chat application featuring a dark mode user interface, real-time messaging with delivery/read confirmations, file sharing, profile management, persistent storage via SQLite, and smooth loader animations.

## Features

- **Dark Mode UI:**  
  A stylish, modern dark theme with accent colors, large fonts, and clear, well-spaced UI elements powered by [FlatLaf].

- **User Registration & Login:**  
  Users can register with their name, username, and password. User credentials are stored persistently in an SQLite database via JDBC.

- **Profile Management:**  
  Users can update their profile information (name, username, password, and profile photo). Changes are reflected dynamically across the application.

- **Real-Time Messaging:**  
  Messages are sent in real time using socket-based communication. A single tick (✔) indicates delivery, and double ticks (✔✔) indicate that the message has been read.

- **File Sharing:**  
  Users can send files of any type. The sender’s message shows the file name, and the recipient sees a **Download** button to save the file.

- **Refresh Chat:**  
  A **REFRESH CHAT** feature reloads contacts and chat history from the database.

- **Loader Animations:**  
  Loader dialogs appear during transitions (e.g., login, registration) to enhance user experience.

- **Unread Message Indicators:**  
  The contacts list displays the number of unread messages and a snippet of the most recent unread message or file name.

## Project Structure

├── ChatServer.java           // Server that relays messages, manages groups, and handles offline messages.
├── lib
      ├── flatlaf-3.5.4.jar
      ├── sqlite-jdbc-3.49.1.0.jar
      ├── trident.jar
├── Client
          ├── ChatClientFrame.java      // Main client GUI with dark mode UI and advanced features.
          ├── Friend.java               // Class managing friend-related operations.
          ├── LoginPanel.java           // UI panel for user login.
          ├── RegistrationPanel.java    // UI panel for new user registration.
          ├── ProfilePanel.java         // UI panel for profile management.
          ├── MessageData.java          // Data model class for chat messages.
          ├── NetworkClient.java        // Handles socket communication between client and server.
          ├── SQLDatabase.java          // Manages SQLite database interactions (users, messages, friends).
          ├── UIStyles.java             // Contains constants and methods for UI styling.
          └── UserData.java             // Data model for user information, chat history, and unread messages.

Technologies Used
Java SE: Sockets, I/O, and Serialization.

Swing: For building the graphical user interface.

JDBC with SQLite: For structured, persistent data storage.

FlatLaf: For modern dark mode UI styling.

Trident: For smooth animations and transitions.

Markdown: For project documentation.

***Prerequisites***
Java 8 or later

Basic knowledge of running Java applications from the command line or within an IDE.

SQLite JDBC driver (e.g., sqlite-jdbc-3.49.1.0.jar) should be available in the lib directory or classpath.

***Installation & Setup***
Clone or Download this repository to your local machine.

Ensure the following files are in the project directory:

- ChatServer.java

*Folder name- Client*

- ChatClientFrame.java

- ChatMainPanel.java

- Friend.java

- LoginPanel.java

- RegistrationPanel.java

- ProfilePanel.java

- MessageData.java

- NetworkClient.java

- SQLDatabase.java

- UIStyles.java

- UserData.java

*Folder name- lib*

- flatlaf-3.5.4.jar

- sqlite-jdbc-3.49.1.0.jar

- trident.jar

Compile the project: Open a terminal in the project directory and run:

bash
```
javac -cp ".;lib/sqlite-jdbc-3.49.1.0.jar" *.java
```

**Running the Application**

Start the Server
Open a terminal and run:

bash
```
java -cp ".;lib/sqlite-jdbc-3.49.1.0.jar" ChatServer
```
The server will start listening on port 12345.


Start the Client
In another terminal (or multiple terminals for multiple users), run:

bash
```
java -cp ".;lib/sqlite-jdbc-3.49.1.0.jar" ChatClientFrame
```

The client GUI will launch in dark mode.

***Usage -***

**Registration**
On the login screen, click the REGISTER button.

Fill in your Name, Username, and Password.

Click REGISTER to create your account, then click BACK TO LOGIN to return.

**Login**
Enter your username and password on the login screen and click LOGIN.

A loader dialog appears for 2 seconds before transitioning to the chat screen.

**Profile Management**
In the chat screen, click the PROFILE button (if available) to update your profile.

Update your name, username, password, and choose a profile photo.

Click SAVE CHANGES to update your profile. The changes will update dynamically throughout the app.

**Chat**
The main chat screen displays a list of contacts with unread message counts and snippets.

Double-click a contact to open the conversation window.

*Sending a Message:*
Type your message and click SEND. Your message initially shows with a pending status; it then updates to a single tick (✔) upon delivery, and double ticks (✔✔) when read.

*File Sharing:*
Click SEND FILE to choose a file. The file is sent, and the recipient sees a Download button to save it.

*Close Chat:*
Click CLOSE CHAT to exit the conversation and return to the contacts list.

*Refresh Chat:*
Click REFRESH CHAT to reload contacts and chat history from the database.

*Logout*
Click LOGOUT to return to the login screen.

*Troubleshooting*
Ambiguous Timer Error:
If you encounter an error like "reference to Timer is ambiguous," ensure that you fully qualify the Swing timer as javax.swing.Timer or remove any conflicting imports from java.util.Timer.

*UI Color Issues:*
Adjust the color constants (e.g., DARK_BG, ACCENT_COLOR) in UIStyles.java to suit your preferences.

*Port Conflicts:*
If the server fails to start because port 12345 is in use, change the port number in ChatServer.java or free the port on your system.
