# Dark Mode Chat Application

A modern Java Swing-based chat application featuring a dark mode user interface, real-time messaging with delivery/read confirmations, file sharing, profile management, persistent storage, and loader animations.

## Features

- **Dark Mode UI:**  
  A stylish, modern dark theme with accent colors, large fonts, and clear, well-spaced UI elements.

- **User Registration & Login:**  
  Users can register with their name, username, and password. All user data is stored persistently in `chatapp_data.ser`.

- **Profile Management:**  
  Users can update their profile (name, username, password, and profile photo). Changes update dynamically across the application.

- **Real-Time Messaging:**  
  Messages are sent in real time using a simple protocol. A single tick (✔) indicates delivery, and double ticks (✔✔) indicate that the message has been read.

- **File Sharing:**  
  Users can send files (any type). The sender’s message shows the file name, and the recipient sees a **Download** button to save the file when desired.

- **Refresh Chat:**  
  A **REFRESH CHAT** button reloads the contacts and chat history from persistent storage.

- **Loader Animations:**  
  A loader dialog is displayed for 2 seconds during transitions (e.g., when logging in or registering) to improve the user experience.

- **Close Chat Window:**  
  Users can close an open chat to return to the contacts list without losing chat history.

- **Unread Message Indicators:**  
  The contacts list shows the number of unread messages (displayed in a small circle) and a snippet of the most recent unread message or file name.

## Project Structure

. ├── ChatServer.java // Server that relays messages and acknowledgments. ├── ChatClient.java // Swing-based client with dark mode UI and advanced features. ├── Message.java // Data model class for chat messages. ├── chatapp_data.ser // Serialized file for persistent user and chat data. ├── README.md // This documentation file.


## Technologies Used

- **Java SE:** Sockets, I/O, and Serialization.
- **Swing:** For building the graphical user interface.
- **Markdown:** For project documentation.

## Prerequisites

- **Java 8 or later.**
- Basic knowledge of running Java applications from the command line or an IDE.

## Installation & Setup

1. **Clone or Download** this repository to your local machine.

2. Ensure the following files are in the same directory:
   - `ChatServer.java`
   - `ChatClient.java`
   - `Message.java`
   - (Optional) `chatapp_data.ser` if pre-existing data is available.

3. **Compile the project:**
   Open a terminal in the project directory and run:
   ```bash
   javac Message.java ChatServer.java ChatClient.java

Running the Application
Start the Server
Open a terminal and run:
bash
java ChatServer
The server will start listening on port 12345.
Start the Client
In another terminal (or multiple terminals for multiple users), run:
bash
java ChatClient
The client GUI will launch in dark mode.

Usage
Registration:

On the login screen, click the REGISTER button.
Fill in your Name, Username, and Password.
Click REGISTER to create your account, then click BACK TO LOGIN to return.
Login:

Enter your username and password on the login screen and click LOGIN.
A loader dialog appears for 2 seconds before transitioning to the chat screen.
Profile Management:

In the chat screen, click the PROFILE button (if available) to update your profile.
Change your name, username, password, and choose a profile photo.
Click SAVE CHANGES to update your profile. The changes will update dynamically throughout the app.
Chat:

The main chat screen shows a list of contacts with unread message counts and snippets.
Double-click a contact to open the conversation window.
Sending a Message: Type your message and click Send. Your message shows with a pending status, then updates to a single tick (✔) when delivered, and double ticks (✔✔) when read.
File Sharing: Click Send File to choose a file. The file is sent, and the recipient sees a Download button to save it when desired.
Close Chat: Click Close Chat to exit the conversation and return to the contacts list.
Refresh Chat: Click REFRESH CHAT to reload the contacts list and chat history from disk.
Logout:

Click LOGOUT to return to the login screen.

Troubleshooting
Ambiguous Timer Error:
If you see an error like "reference to Timer is ambiguous," ensure you either fully qualify the Swing timer as javax.swing.Timer or remove any conflicting imports from java.util.Timer.

UI Color Issues:
Adjust the color constants (e.g., DARK_BG, ACCENT_COLOR) in ChatClient.java to suit your preferences.

Port Conflicts:
If the server fails to start because port 12345 is in use, change the port number in ChatServer.java or free the port on your system.

Contributing
Contributions are welcome! Feel free to fork the project and submit pull requests with improvements or additional features, such as:

Enhanced animations and transitions.
Improved error handling and UI refinements.
Support for group chats, encryption, and advanced file transfer methods.

License
This project is provided for educational purposes. You are free to use and modify it as needed. If you create a public fork, please attribute the original source.

