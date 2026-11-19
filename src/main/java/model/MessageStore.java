
package model;

import java.util.List;

// Where messages for agents are kept (Member D).
// The menu only talks to this interface,
// so its tests can use a Mockito mock.
public interface MessageStore {

    // Saves a message from the logged-in user
    // to the agent with this name (any case).
    // Throws IllegalArgumentException if there is
    // no such agent or the text is blank.
    Message send(String sender, String agentName, String text);

    // Messages for the agent whose name matches
    // this username, ignoring case.
    // Empty if the user is not an agent or has no messages.
    // Oldest first.
    List<Message> getMessagesFor(String username);

    // HW7 Part C: Retrieve one message.
    // Only the intended recipient can access it.
    // Returns null if the message does not exist
    // or does not belong to the username.
    Message getMessage(int messageId, String username);

    // HW7 Part C: Mark a message as read in the database.
    // Only the intended recipient can mark it as read.
    void markAsRead(int messageId, String username);
}
