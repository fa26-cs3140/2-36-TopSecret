import java.io.IOException;
import java.util.Scanner;

public class TopSecret {

    public static void main(String[] args) {

        CredentialManager manager = new CredentialManager();
        Scanner scanner = new Scanner(System.in);

        LoginHandler loginHandler =
                new LoginHandler(manager, scanner);

        try {

            // Allow the user to change their password.
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

            // Require login before accessing mission briefs.
            boolean loggedIn = loginHandler.authenticate();

            if (!loggedIn) {
                return;
            }

            System.out.println("Access granted.");

            // Start Team Member D's interactive mission menu.
            ProgramControl programControl = new ProgramControl();
            UserInterface ui = new UserInterface(programControl);
            ui.run(args);

        } catch (IOException | IllegalArgumentException e) {
            System.out.println(
                    "Error: " + e.getMessage()
            );
        }
    }
}