import java.io.IOException;
import java.util.Scanner;

public class LoginHandler {

    private final CredentialManager manager;
    private final Scanner scanner;

    public LoginHandler(CredentialManager manager, Scanner scanner) {
        this.manager = manager;
        this.scanner = scanner;
    }

    // Account setup on first run, login on later runs.
    public boolean authenticate() throws IOException {

        if (!manager.credentialsExist()) {
            System.out.println("No credentials found.");
            System.out.println("Create a new account.");

            System.out.print("New username: ");
            String username = scanner.nextLine();

            System.out.print("New password: ");
            String password = scanner.nextLine();

            if (!manager.isValidUsername(username)
                    || !manager.isValidPassword(password)) {
                System.out.println("Invalid username or password.");
                return false;
            }

            manager.createCredentials(username, password);

            System.out.println("Account created successfully.");
            System.out.println("Restart the program to log in.");
            return false;
        }

        if (!manager.hasValidCredentialFile()) {
            System.out.println("Invalid credentials file.");
            return false;
        }

        System.out.print("Username: ");
        String username = scanner.nextLine();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        if (manager.login(username, password)) {
            System.out.println("Login successful!");
            return true;
        }

        System.out.println("Incorrect username or password.");
        return false;
    }

    // Command-line password changing.
    public void changePassword() throws IOException {

        if (!manager.hasValidCredentialFile()) {
            System.out.println("No valid credentials available.");
            return;
        }

        System.out.print("Username: ");
        String username = scanner.nextLine();

        System.out.print("Current password: ");
        String oldPassword = scanner.nextLine();

        System.out.print("New password: ");
        String newPassword = scanner.nextLine();

        System.out.print("Confirm new password: ");
        String confirmation = scanner.nextLine();

        if (manager.changePassword(
                username,
                oldPassword,
                newPassword,
                confirmation)) {

            System.out.println("Password changed successfully.");
        } else {
            System.out.println("Password change failed.");
        }
    }
}