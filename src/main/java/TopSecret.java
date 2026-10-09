import java.io.IOException;
import java.util.Scanner;

public class TopSecret {

    public static void main(String[] args) {

        CredentialManager manager = new CredentialManager();
        Scanner scanner = new Scanner(System.in);

        LoginHandler loginHandler =
                new LoginHandler(manager, scanner);

        try {

            // Allow users to change their password.
            if (args.length == 1
                    && args[0].equals("--change-password")) {
                loginHandler.changePassword();
                return;
            }

            // Reject unknown command-line arguments.
            if (args.length != 0) {
                System.out.println("Unknown command.");
                return;
            }

            // Require authentication before mission access.
            boolean loggedIn = loginHandler.authenticate();

            if (!loggedIn) {
                return;
            }

            System.out.println("Access granted.");

            // Team Member D will connect the mission menu here
            // once UserInterface.java has been updated.

        } catch (IOException | IllegalArgumentException e) {
            System.out.println(
                    "Authentication error: " + e.getMessage()
            );
        }
    }
}