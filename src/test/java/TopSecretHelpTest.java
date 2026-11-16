//import static org.junit.jupiter.api.Assertions.*;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.InputStream;
//import java.io.PrintStream;
//import java.nio.charset.StandardCharsets;
//import org.junit.jupiter.api.Test;
//
//// HW7 Member A: the command line offers -h.
//// Contract: public static String TopSecret.helpText();  main(new String[]{"-h"}) prints it and returns
//// before any login prompt. -h with other arguments is still "Unknown command." like any bad argument.
//class TopSecretHelpTest {
//
//    // Runs main with no keyboard input, so a login prompt would end the program instead of hanging the test.
//    private String runMain(String... args) {
//        PrintStream originalOut = System.out;
//        InputStream originalIn = System.in;
//        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//        try {
//            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
//            System.setIn(new ByteArrayInputStream(new byte[0]));
//            TopSecret.main(args);
//        } finally {
//            System.setOut(originalOut);
//            System.setIn(originalIn);
//        }
//        return buffer.toString(StandardCharsets.UTF_8);
//    }
//
//    @Test
//    void helpTextIsNotBlank() {
//        assertFalse(TopSecret.helpText().isBlank());
//    }
//
//    @Test
//    void helpTextMentionsHelpSwitch() {
//        assertTrue(TopSecret.helpText().contains("-h"));
//    }
//
//    @Test
//    void helpTextMentionsChangePasswordSwitch() {
//        assertTrue(TopSecret.helpText().contains("--change-password"));
//    }
//
//    @Test
//    void helpTextMentionsWebSwitch() {
//        assertTrue(TopSecret.helpText().contains("-web"));
//    }
//
//    @Test
//    void dashHPrintsTheHelpText() {
//        String output = runMain("-h");
//        assertTrue(output.contains(TopSecret.helpText().trim()));
//    }
//
//    @Test
//    void dashHDoesNotLogInOrOpenTheMenu() {
//        String output = runMain("-h");
//        assertFalse(output.contains("Access granted"));
//        assertFalse(output.contains("TOP SECRET SYSTEM"));
//        assertFalse(output.contains("Unknown command"));
//    }
//
//    @Test
//    void dashHWithOtherArgumentsIsAnUnknownCommand() {
//        String output = runMain("-h", "extra");
//        assertTrue(output.contains("Unknown command."));
//        assertFalse(output.contains(TopSecret.helpText().trim()));
//    }
//
//    @Test
//    void unknownArgumentDoesNotPrintHelp() {
//        String output = runMain("--bogus");
//        assertTrue(output.contains("Unknown command."));
//    }
//}