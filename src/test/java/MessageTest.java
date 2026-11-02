import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    private final Message message =
            new Message(4, "Ghost", "smayan", "Meet at the safehouse.", "2026-10-29T14:10:00");

    @Test
    void constructorAndGettersReturnValues() {
        assertEquals(4, message.getId());
        assertEquals("Ghost", message.getAgentName());
        assertEquals("smayan", message.getSender());
        assertEquals("Meet at the safehouse.", message.getText());
        assertEquals("2026-10-29T14:10:00", message.getSentAt());
    }

    @Test
    void toStringShowsTimeSenderAndText() {
        assertEquals("2026-10-29T14:10:00  from smayan: Meet at the safehouse.", message.toString());
    }
}
