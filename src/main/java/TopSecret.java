import java.io.UncheckedIOException;
import java.nio.file.Path;
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
            // Start web mode before terminal authentication.
            if (args.length == 1 && args[0].equals("-web")) {
                if (!accounts.hasAccounts()) {
                    System.out.println(
                            "Create a user account in terminal mode first.");
                    return;
                }

                try {
                    MissionStore webStore = new SqliteMissionStore();

                    AuditLog webAudit = new SqliteAuditLog(
                            SqliteMissionStore.DEFAULT_DB_FILE);

                    Connection webConnection = DriverManager.getConnection(
                            "jdbc:sqlite:" + SqliteMissionStore.DEFAULT_DB_FILE);

                    try {
                        ModelDatabase webDatabase =
                                new ModelDatabase(webConnection);

                        webDatabase.createTables();
                        webDatabase.importSampleData(Path.of("database"));

                        WebController web = new WebController(
                                accounts,
                                new WebPages(webConnection, webStore, webAudit));

                        web.start();

                        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                            web.stop();

                            try {
                                webConnection.close();
                            } catch (SQLException ignored) {
                            }
                        }));

                        System.out.println(
                                "Web interface: http://localhost:3002/");

                    } catch (SQLException | RuntimeException e) {
                        webConnection.close();
                        throw e;
                    }

                } catch (SQLException e) {
                    System.out.println("Database error: " + e.getMessage());
                }

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
                database.importSampleData(Path.of("database"));

                Homework5Menu homework5Menu = new Homework5Menu(
                        database,
                        scanner,
                        System.out,
                        audit,
                        accounts,
                        user,
                        user.isAdmin()
                );

                // Messages for agents (Member D). Uses the same database file.
                MessageMenu messageMenu = new MessageMenu(
                        new SqliteMessageStore(SqliteMissionStore.DEFAULT_DB_FILE),
                        database, audit, user, scanner, System.out);
                messageMenu.printInboxNotice();

                // Missions and agent assignments (Member A), admins only
                AssignmentMenu assignmentMenu = new AssignmentMenu(
                        database, scanner, System.out, audit);

                while (true) {
                    System.out.println("\n=== TOP SECRET SYSTEM ===");
                    System.out.println("1. Existing Mission Menu");
                    System.out.println("2. Homework 5 Records Menu");
                    System.out.println("3. Messages");
                    if (user.isAdmin()) {
                        System.out.println("4. Missions and Assignments");
                    }
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

                        case "3" -> messageMenu.run();

                        case "4" -> {
                            if (user.isAdmin()) {
                                assignmentMenu.run(user.getUsername());
                            } else {
                                System.out.println("Invalid choice. Try again.");
                            }
                        }

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
