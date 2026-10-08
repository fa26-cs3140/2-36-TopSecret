import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserInterfaceTest {

    @Test
    void canMakeUserInterface() {
        // Check that we can create a UserInterface object
        UserInterface ui = new UserInterface(new ProgramControl());
        assertNotNull(ui);
    }

    @Test
    void noInputDoesNotCrash() {
        // Check that running with nothing typed in does not crash
        UserInterface ui = new UserInterface(new ProgramControl());
        try {
            ui.run(new String[]{});
        } catch (Exception e) {
            fail("Program crashed with no input");
        }
    }

    @Test
    void lettersInsteadOfNumberDoesNotCrash() {
        // Check that typing letters instead of a number does not crash
        UserInterface ui = new UserInterface(new ProgramControl());
        try {
            ui.run(new String[]{"abc"});
        } catch (Exception e) {
            fail("Program crashed with letters instead of a number");
        }
    }

    @Test
    void fileNumberTooHighDoesNotCrash() {
        // Check that asking for a file number that does not exist does not crash
        UserInterface ui = new UserInterface(new ProgramControl());
        try {
            ui.run(new String[]{"999"});
        } catch (Exception e) {
            fail("Program crashed with a file number that does not exist");
        }
    }

    @Test
    void validFileNumberDoesNotCrash() {
        // Check that asking for a file number that does exist does not crash
        UserInterface ui = new UserInterface(new ProgramControl());
        try {
            ui.run(new String[]{"1"});
        } catch (Exception e) {
            fail("Program crashed with a valid file number");
        }
    }
}