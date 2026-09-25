import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserInterfaceTest {

    @Test
    void userInterfaceCanBeCreated() {
        UserInterface ui = new UserInterface(new ProgramControl());

        assertNotNull(ui);
    }

//    @Test
//    void noArgumentsDoesNotCrash() {
//        UserInterface ui = new UserInterface(new ProgramControl());
//
//        assertDoesNotThrow(() -> ui.run(new String[]{}));
//    }
//
//    @Test
//    void invalidFileNumberDoesNotCrash() {
//        UserInterface ui = new UserInterface(new ProgramControl());
//
//        assertDoesNotThrow(() -> ui.run(new String[]{"abc"}));
//    }
}