import java.io.UncheckedIOException;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TopSecret {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            AccountManager accounts = new AccountManager(new CipheredUserStore());
            LoginHandler loginHandler = new LoginHandler(accounts, scanner, System.out);

            // Allow users to change their password.
            if (args.length == 1 && args[0].equals("--change-password")) {
                loginHandler.changePassword();
                return;
            }

            // Reject unknown command-line arguments.
            if (args.length != 0) {
                System.out.println("Unknown command.");
                return;
            }

            // Require login before accessing mission briefs.
            UserAccount user = loginHandler.authenticate();
            if (user == null) {
                return;
            }

            System.out.println("Access granted.");

            // Start the interactive menu. The database supplies the missions and search,
            // and every read or create is written to the audit log for this user.

            MissionStore store = new SqliteMissionStore();
            AuditLog audit = new SqliteAuditLog(
                    SqliteMissionStore.DEFAULT_DB_FILE);

            try (Connection connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + SqliteMissionStore.DEFAULT_DB_FILE)) {

                ModelDatabase database = new ModelDatabase(connection);
                database.createTables();

                Homework5Menu homework5Menu = new Homework5Menu(
                        database,
                        scanner,
                        System.out,
                        audit,
                        accounts,
                        user,
                        user.isAdmin()
                );

                while (true) {
                    System.out.println("\n=== TOP SECRET SYSTEM ===");
                    System.out.println("1. Existing Mission Menu");
                    System.out.println("2. Homework 5 Records Menu");
                    System.out.println("0. Logout");
                    System.out.print("Choose an option: ");

                    if (!scanner.hasNextLine()) {
                        break;
                    }

                    String choice = scanner.nextLine().trim();

                    switch (choice) {
                        case "1" -> new MissionMenu(
                                store, store, scanner, System.out,
                                audit, accounts, user).run();

                        case "2" -> homework5Menu.run();

                        case "0" -> {
                            System.out.println("Goodbye!");
                            return;
                        }

                        default -> System.out.println(
                                "Invalid choice. Try again.");
                    }
                }

            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }

        } catch (IllegalArgumentException | IllegalStateException | UncheckedIOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
