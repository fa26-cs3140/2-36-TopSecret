import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BriefTest {

    @Test
    void constructorAndGetters() {
        Brief b = new Brief(5, "Project Bluebird", "1973-04-20", "Exfiltrate the defector.");
        assertEquals(5, b.getId());
        assertEquals("Project Bluebird", b.getTitle());
        assertEquals("1973-04-20", b.getDate());
        assertEquals("Exfiltrate the defector.", b.getText());
    }

    @Test
    void settersChangeValues() {
        Brief b = new Brief(1, "Old", "2000-01-01", "old text");
        b.setTitle("New");
        b.setDate("2001-02-02");
        b.setText("new text");
        assertEquals("New", b.getTitle());
        assertEquals("2001-02-02", b.getDate());
        assertEquals("new text", b.getText());
    }
}
