package ServerApp.ClientHandler2;

import ServerApp.Server2.Server2;
import ServerApp.MessageHandler.MessageHandler;
import ServerApp.Admin.Admin;
import ServerApp.AuthenticationSystem.AuthenticationSystem;
import ServerApp.ChatBox.ChatBox;
import ServerApp.User.User;
import Common.MessageInterface;
import Common.Messages.*;

import java.net.Socket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class ClientHandler2 implements Runnable {

	// Attributes
	private Socket clientSocket;
	private Server2 server;
	private MessageHandler messageHandler;
	private AuthenticationSystem authenticationSystem;
	private User user;
	private ObjectInputStream input;
	private ObjectOutputStream output;
	private volatile boolean isRunning;

	// Constructor
	public ClientHandler2(Socket clientSocket, Server2 server, MessageHandler messageHandler,
			AuthenticationSystem authenticationSystem) {
		this.clientSocket = clientSocket;
		this.server = server;
		this.messageHandler = messageHandler;
		this.authenticationSystem = authenticationSystem;
		this.isRunning = true;
		try {
			this.output = new ObjectOutputStream(clientSocket.getOutputStream());
			this.input = new ObjectInputStream(clientSocket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
			closeConnection();
		}
	}

	// Handles client communication and requests
	@Override
	public void run() {
		try {
			// Authentication Loop
			while (isRunning && user == null) {
				MessageInterface request = (MessageInterface) input.readObject();
				handleMessage(request);
			}

			// Main communication loop
			while (isRunning) {
				MessageInterface request = (MessageInterface) input.readObject();
				handleMessage(request);
			}

		} catch (IOException | ClassNotFoundException e) {
			System.err.println("Connection error with client: " + e.getMessage());
		} finally {
			closeConnection();
		}
	}

	// Handles different message types
	private void handleMessage(MessageInterface message) {
		switch (message.getType()) {
		//case LOGIN -> handleLogin((Login) message);
		//case CREATE_USER -> handleCreateUser((CreateUser) message);
		case LOGIN -> handleLogin((Login) message);
		case CREATE_USER -> handleCreateUser((CreateUser) message);
		case SEND_MESSAGE -> handleSendMessage((SendMessage) message);
		case LOGOUT -> handleLogout();
		default -> sendNotification("Unknown message type received.");
		}
	}

	// Handle Login
	private void handleLogin(Login login) {
		String username = login.username();
		String password = login.password();

		// Validate credentials using AuthenticationSystem
		User authenticatedUser = authenticationSystem.validateCredentials(username, password);

		if (authenticatedUser != null) {
			// Successful login
			this.user = authenticatedUser;
			System.out.println("User logged in: " + user.getUsername());

			// Retrieve all ChatBoxes the user is part of
			List<ChatBox> userChatBoxes = server.getChatBoxes().values().stream()
					.filter(chatBox -> chatBox.getParticipants().contains(user)).map(ChatBox::getEmpty).toList();

			// Create and send LoginResponse
			LoginResponse loginResponse = new LoginResponse(user, userChatBoxes);
			sendMessage(loginResponse);
		} else {
			// Failed login
			System.out.println("Failed login attempt for username: " + username);

			// Create and send LoginResponse indicating failure
			LoginResponse loginResponse = new LoginResponse(null, null);
			sendMessage(loginResponse);
		}
	}

	
    private void handleCreateUser(CreateUser createUser) {
        // Step 1: Check if the requesting user is an admin
        if (!(user instanceof Admin)) {
            System.out.println("User " + (user != null ? user.getUsername() : "Unknown") + " attempted to create a user without admin privileges.");

            // Send a CreateUserResponse indicating denial
            Notification response = new Notification("Access denied. Admin privileges required to create a new user.");
            sendMessage(response);
            return;
        }

        // Step 2: Proceed with user registration
        String newUsername = createUser.username();
        String newPassword = createUser.password();

        // Create a new User instance
        User newUser = new User(newUsername, newPassword);

        // Attempt to register the new user
        boolean registrationSuccess = authenticationSystem.registerUser(newUser);

        if (registrationSuccess) {
            System.out.println("Admin " + user.getUsername() + " successfully created user: " + newUsername);

            // Send a CreateUserResponse indicating success
            Notification response = new Notification("User created successfully.");
            sendMessage(response);
        } else {
            System.out.println("Admin " + user.getUsername() + " failed to create user: " + newUsername);

            // Send a CreateUserResponse indicating failure (e.g., username already exists)
            Notification response = new Notification("Failed to create user. Username may already exist.");
            sendMessage(response);
        }
    }
  
	
	// Handle SendMessage
	private void handleSendMessage(SendMessage sendMessage) {
		boolean success = messageHandler.sendMessage(sendMessage.chatBoxID(), sendMessage.message());
		if (!success) {
			sendNotification("Failed to send message.");
		}
	}

	// Handle Logout
	private void handleLogout() {
		authenticationSystem.logout(user.getUserID());
		sendNotification("Logout successful.");
		closeConnection();
	}

	// Send a message to the client
	private void sendMessage(MessageInterface message) {
		try {
			output.writeObject(message);
			output.flush();
		} catch (IOException e) {
			System.err.println("Error sending message to client: " + e.getMessage());
		}
	}

	// Send a notification to the client
	private void sendNotification(String text) {
		Notification notification = new Notification(text);
		sendMessage(notification);
	}

	// Closes the client connection and cleans up resources
	public void closeConnection() {
		isRunning = false;
		try {
			if (input != null)
				input.close();
			if (output != null)
				output.close();
			if (clientSocket != null && !clientSocket.isClosed())
				clientSocket.close();
		} catch (IOException e) {
			System.err.println("Error closing client connection: " + e.getMessage());
		} finally {
			server.removeClientHandler(this);
			if (user != null) {
				authenticationSystem.logout(user.getUserID());
			}
		}
	}

	// Retrieves the client socket
	public Socket getClientSocket() {
		return clientSocket;
	}

	// Retrieves the user associated with this client
	public User getUser() {
		return user;
	}

	// Sets the user associated with this client
	public void setUser(User user) {
		this.user = user;
	}

	public void sendChatBoxUpdate(ChatBox chatBox) {
		// TODO Auto-generated method stub

	}
}
