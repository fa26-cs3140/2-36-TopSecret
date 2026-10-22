import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserAccountTest {

    @Test
    void constructorAndGettersReturnValues() {
        UserAccount u = new UserAccount("agent_1", "Secret1", true);
        assertEquals("agent_1", u.getUsername());
        assertEquals("Secret1", u.getPassword());
        assertTrue(u.isAdmin());
    }

    @Test
    void setPasswordChangesPassword() {
        UserAccount u = new UserAccount("agent", null, false);
        u.setPassword("NewPass");
        assertEquals("NewPass", u.getPassword());
    }

    @Test
    void setAdminChangesAccessFlag() {
        UserAccount u = new UserAccount("agent", "Secret1", false);
        u.setAdmin(true);
        assertTrue(u.isAdmin());
        u.setAdmin(false);
        assertFalse(u.isAdmin());
    }

    @Test
    void hasPasswordIsFalseUntilPasswordIsSet() {
        assertFalse(new UserAccount("agent", null, false).hasPassword());
        assertFalse(new UserAccount("agent", "", false).hasPassword());
        assertTrue(new UserAccount("agent", "Secret1", false).hasPassword());
    }
}
