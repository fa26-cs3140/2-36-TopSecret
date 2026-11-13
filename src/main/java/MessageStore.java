//Defines operations for sending, retrieving, and marking messages as read.

import java.util.List;

public interface MessageStore {

    void sendMessage(
            String sender,
            String recipient,
            String content
    );

    List<Message> getMessagesFor(String username);

    Message getMessage(int messageId, String username);

    void markAsRead(int messageId, String username);
}