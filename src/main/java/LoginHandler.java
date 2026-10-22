import java.io.PrintStream;
import java.util.Scanner;

// Console prompts for accounts (Member D, built on Member B's Homework 4 login):
// first-run setup, login, first-login password, and changing a password.
public class LoginHandler {

    private final AccountManager accounts;
    private final Scanner in;
    private final PrintStream out;

    public LoginHandler(AccountManager accounts, Scanner in, PrintStream out) {
        this.accounts = accounts;
        this.in = in;
        this.out = out;
    }

    // Returns the logged-in user, or null if login did not succeed.
    // With no accounts yet, creates the first (admin) account and returns null
    // so the program exits, as in Homework 4.
    public UserAccount authenticate() {
        if (!accounts.hasAccounts()) {
            createFirstAccount();
            return null;
        }

        String username = ask("Username: ");
        if (username == null) {
            return null;
        }

        if (accounts.needsPassword(username)) {
            return setFirstPassword(username);
        }

        String password = ask("Password: ");
        UserAccount user = password == null ? null : accounts.login(username, password);
        if (user == null) {
            out.println("Incorrect username or password.");
            return null;
        }
        out.println("Login successful!");
        return user;
    }

    // For: java TopSecret --change-password
    public boolean changePassword() {
        String username = ask("Username: ");
        String oldPassword = ask("Current password: ");
        String newPassword = ask("New password: ");
        String confirm = ask("Confirm new password: ");
        if (confirm != null && accounts.changePassword(username, oldPassword, newPassword, confirm)) {
            out.println("Password changed successfully.");
            return true;
        }
        out.println("Password change failed. " + AccountManager.PASSWORD_RULE);
        return false;
    }

    private void createFirstAccount() {
        out.println("No user accounts found. Create the first account (it will be an admin).");
        out.println(AccountManager.USERNAME_RULE);
        out.println(AccountManager.PASSWORD_RULE);
        String username = ask("New username: ");
        String password = ask("New password: ");
        String confirm = ask("Confirm password: ");
        if (confirm == null) {
            return;
        }
        if (!password.equals(confirm)) {
            out.println("Passwords do not match.");
            return;
        }
        try {
            accounts.createFirstAccount(username, password);
            out.println("Account created. Restart the program to log in.");
        } catch (IllegalArgumentException e) {
            out.println(e.getMessage());
        }
    }

    private UserAccount setFirstPassword(String username) {
        out.println("Welcome, " + username + ". Create your password.");
        out.println(AccountManager.PASSWORD_RULE);
        String password = ask("New password: ");
        String confirm = ask("Confirm password: ");
        if (confirm != null && accounts.setInitialPassword(username, password, confirm)) {
            out.println("Password set. Login successful!");
            return accounts.findUser(username);
        }
        out.println("Password not set. Passwords must match and follow the rules.");
        return null;
    }

    // Prints the prompt and reads a line, or returns null if there is no more input
    private String ask(String prompt) {
        out.print(prompt);
        return in.hasNextLine() ? in.nextLine() : null;
    }
}
