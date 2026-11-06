package view;

import model.Access;
import java.io.PrintStream;
import java.util.Map;
import java.util.Scanner;

// Admin-only menu to list, promote and demote users
public class AccessMenu {

    private final Access access;
    private final Scanner in;
    private final PrintStream out;

    public AccessMenu(Access access, Scanner in, PrintStream out) {
        this.access = access;
        this.in = in;
        this.out = out;
    }

    public void run(String actingUser) {
        if (!access.isAdmin(actingUser)) {
            out.println("Access denied.");
            return;
        }
        while (true) {
            showMenu();
            if (!in.hasNextLine()) {
                return;
            }
            String choice = in.nextLine().trim();
            if (choice.equals("1")) {
                listUsers();
            } else if (choice.equals("2")) {
                promoteUser(actingUser);
            } else if (choice.equals("3")) {
                demoteUser(actingUser);
            } else if (choice.equals("4")) {
                return;
            } else {
                out.println("Invalid choice. Enter 1, 2, 3, or 4.");
            }
        }
    }

    void showMenu() {
        out.println();
        out.println("1. List users");
        out.println("2. Promote a user to admin");
        out.println("3. Demote an admin to standard user");
        out.println("4. Back");
        out.print("Choose an option: ");
    }

    void listUsers() {
        Map<String, String> users = access.getAllUsers();
        if (users.isEmpty()) {
            out.println("No users.");
        }
        for (String name : users.keySet()) {
            out.println(name + " - " + users.get(name));
        }
    }

    void promoteUser(String actingUser) {
        String target = ask("Username to promote: ");
        try {
            access.promote(actingUser, target);
            out.println(target + " is now an admin.");
        } catch (IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
        }
    }

    void demoteUser(String actingUser) {
        String target = ask("Username to demote: ");
        try {
            access.demote(actingUser, target);
            out.println(target + " is now a standard user.");
        } catch (IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
        }
    }

    private String ask(String question) {
        out.print(question);
        return in.hasNextLine() ? in.nextLine().trim() : "";
    }
}