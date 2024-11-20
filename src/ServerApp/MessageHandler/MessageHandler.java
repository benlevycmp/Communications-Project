package ServerApp.MessageHandler;

import ServerApp.StorageManager.StorageManager;
import ServerApp.ChatBox.ChatBox;
import ServerApp.Message.Message;
import ServerApp.ClientHandler2.ClientHandler2;
import ServerApp.User.User;
import ServerApp.AuthenticationSystem.AuthenticationSystem;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

/**
 * MessageHandler manages message-related operations, acting as an intermediary
 * between ClientHandler2 and other components.
 */
public class MessageHandler {

    // Attributes
    private StorageManager storageManager; // Manages storage operations for chatboxes
    private ConcurrentHashMap<Integer, ChatBox> chatBoxes; // In-memory chatboxes
    private ConcurrentHashMap<Integer, User> userDB; // In-memory users

    // Constructor
    // *Initializes MessageHandler with storageManager, chatBoxes, and userDB*
    // INPUT: storageManager (StorageManager), chatBoxes (ConcurrentHashMap<Integer, ChatBox>), userDB (ConcurrentHashMap<Integer, User>)
    public MessageHandler(StorageManager storageManager, ConcurrentHashMap<Integer, ChatBox> chatBoxes, ConcurrentHashMap<Integer, User> userDB) {
        this.storageManager = storageManager;
        this.chatBoxes = chatBoxes;
        this.userDB = userDB;
    }

    // *Sends a message to a specific chatbox*
    // INPUT: chatBoxID (int), message (Message)
    // OUTPUT: true if successful, false otherwise
    public boolean sendMessage(int chatBoxID, Message message) {
        ChatBox chatBox = chatBoxes.get(chatBoxID);
        if (chatBox != null) {
            chatBox.addMessage(message);
            storeChatBox(chatBox); // Store updated chatbox in persistent storage
            updateParticipants(chatBoxID); // Update all participants with the new chatbox
            return true;
        }
        return false;
    }

    // *Sends a message to all chatboxes*
    // INPUT: message (Message)
    // OUTPUT: none
    public void sendMessageToAllChatBoxes(Message message) {
        for (ChatBox chatBox : chatBoxes.values()) {
            chatBox.addMessage(message);
            storeChatBox(chatBox); // Store updated chatbox in persistent storage
            updateParticipants(chatBox.getChatBoxID()); // Update all participants with the new chatbox
        }
    }

    // *Sends a message to a specific user*
    // INPUT: userID (int), content (String)
    // OUTPUT: true if message sent successfully, false otherwise
    public boolean sendMessageToUser(int userID, String content) {
        User user = userDB.get(userID);
        if (user != null) {
            Message message = new Message(userID, content);
            // Assuming sending a private message involves adding to a specific chatbox or creating a new one
            // For simplicity, we'll assume a private chatbox exists for the user
            // Implement accordingly based on your chatbox management logic
            // Example:
            int privateChatBoxID = getPrivateChatBoxID(userID);
            if (privateChatBoxID != -1) {
                return sendMessage(privateChatBoxID, message);
            }
        }
        return false;
    }

    // *Adds a user to a specific chatbox*
    // INPUT: chatBoxID (int), clientHandler (ClientHandler2)
    // OUTPUT: true if user added successfully, false otherwise
    public boolean addParticipantToChatBox(int chatBoxID, ClientHandler2 clientHandler) {
        ChatBox chatBox = chatBoxes.get(chatBoxID);
        if (chatBox != null) {
            boolean added = chatBox.addParticipant(clientHandler.getUser());
            if (added) {
                storeChatBox(chatBox); // Store updated chatbox to reflect the new participant
                return true;
            }
        }
        return false;
    }

    // *Removes a user from a specific chatbox*
    // INPUT: chatBoxID (int), userID (int)
    // OUTPUT: true if user removed successfully, false otherwise
    public boolean removeParticipantFromChatBox(int chatBoxID, int userID) {
        ChatBox chatBox = chatBoxes.get(chatBoxID);
        if (chatBox != null) {
            User userToRemove = chatBox.getParticipants().stream()
                .filter(user -> user.getUserID() == userID)
                .findFirst()
                .orElse(null);
            boolean removed = chatBox.removeParticipant(userToRemove);
            if (removed) {
                storeChatBox(chatBox); // Store updated chatbox to reflect the removed participant
                return true;
            }
        }
        return false;
    }

    // *Retrieves all messages for a specific chatbox*
    // INPUT: chatBoxID (int)
    // OUTPUT: List of messages or null if chatbox not found
    public List<Message> getMessages(int chatBoxID) {
        ChatBox chatBox = chatBoxes.get(chatBoxID);
        if (chatBox != null) {
            return chatBox.getMessagesList();
        }
        return null;
    }

    // *Updates all participants in the chatbox with the latest chatbox state*
    // INPUT: chatBoxID (int)
    // OUTPUT: none
    private void updateParticipants(int chatBoxID) {
        ChatBox chatBox = chatBoxes.get(chatBoxID);
        if (chatBox != null) {
            for (User participant : chatBox.getParticipants()) {
                ClientHandler2 clientHandler = findClientHandler(participant.getUserID());
                if (clientHandler != null) {
                    clientHandler.sendChatBoxUpdate(chatBox); // Send the updated chatbox to the client via the client handler
                }
            }
        }
    }

    // *Finds the ClientHandler2 for a given user ID*
    // INPUT: userID (int)
    // OUTPUT: ClientHandler2 or null if not found
    private ClientHandler2 findClientHandler(int userID) {
        // Iterate through client handlers to find the matching userID
        // Assuming Server2 has a method to retrieve client handlers
        // and MessageHandler has a reference to Server2
        // To avoid tight coupling, consider passing Server2 reference or use observer pattern
        // For simplicity, we'll assume MessageHandler has access to Server2's client handlers
        // Modify accordingly based on your actual implementation

        // Example placeholder:
        // return server2Instance.getClientHandlers().stream()
        //         .filter(handler -> handler.getUser().getUserID() == userID)
        //         .findFirst().orElse(null);

        // Since MessageHandler does not have access to Server2's client handlers directly,
        // consider passing a reference or using a callback mechanism.
        // For now, we'll leave this method unimplemented.
        return null;
    }

    // *Loads a chatbox into memory from storage*
    // INPUT: chatBoxID (int)
    // OUTPUT: Loaded ChatBox or null if not found
    public ChatBox loadChatBox(int chatBoxID) {
        ChatBox chatBox = storageManager.retrieveChatBox(chatBoxID);
        if (chatBox != null) {
            chatBoxes.put(chatBoxID, chatBox); // Add the chatbox to the in-memory collection
        }
        return chatBox;
    }

    // *Stores a chatbox in persistent storage*
    // INPUT: chatBox (ChatBox)
    // OUTPUT: true if stored successfully, false otherwise
    public boolean storeChatBox(ChatBox chatBox) {
        if (chatBox != null) {
            chatBoxes.put(chatBox.getChatBoxID(), chatBox); // Update the in-memory collection
            storageManager.storeChatBox(chatBox); // Store the chatbox in persistent storage
            return true;
        }
        return false;
    }

    // *Hides a specific message in a chatbox*
    // INPUT: chatBoxID (int), messageID (int)
    // OUTPUT: true if message hidden successfully, false otherwise
    public boolean hideMessage(int chatBoxID, int messageID) {
        ChatBox chatBox = chatBoxes.get(chatBoxID);
        if (chatBox != null) {
            Message messageToHide = chatBox.getMessages().stream()
                .filter(message -> message.getMessageID() == messageID)
                .findFirst()
                .orElse(null);
            if (messageToHide != null) {
                messageToHide.setHidden(true);
                storeChatBox(chatBox); // Store updated chatbox to reflect hidden message
                updateParticipants(chatBoxID); // Update all participants with the latest chatbox state
                return true;
            }
        }
        return false;
    }

    // *Hides an entire chatbox*
    // INPUT: chatBoxID (int)
    // OUTPUT: true if chatbox hidden successfully, false otherwise
    public boolean hideChatBox(int chatBoxID) {
        ChatBox chatBox = chatBoxes.get(chatBoxID);
        if (chatBox != null) {
            chatBox.hideChatBox(); // Set chatbox to hidden
            storeChatBox(chatBox); // Store updated chatbox to reflect hidden status
            updateParticipants(chatBoxID); // Update all participants with the latest chatbox state
            return true;
        }
        return false;
    }

    // *Helper method to retrieve or create a private chatbox for a user*
    // INPUT: userID (int)
    // OUTPUT: chatBoxID (int) or -1 if failed
    private int getPrivateChatBoxID(int userID) {
        // Implement logic to retrieve or create a private chatbox for the user
        // For example, search for a chatbox with only this user or create a new one
        // Placeholder implementation:
        for (ChatBox chatBox : chatBoxes.values()) {
            if (chatBox.getParticipants().size() == 1 && chatBox.getParticipants().iterator().next().getUserID() == userID) {
                return chatBox.getChatBoxID();
            }
        }
        // If no existing private chatbox, create a new one
        ChatBox newChatBox = ChatBox.createChatBox(List.of(userDB.get(userID)));
        if (storeChatBox(newChatBox)) {
            return newChatBox.getChatBoxID();
        }
        return -1;
    }
}
