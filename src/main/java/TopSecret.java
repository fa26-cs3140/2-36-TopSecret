import java.io.UncheckedIOException;
import java.util.Scanner;

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
            AuditLog audit = new SqliteAuditLog(SqliteMissionStore.DEFAULT_DB_FILE);
            new MissionMenu(store, store, scanner, System.out, audit, accounts, user).run();

        } catch (IllegalArgumentException | IllegalStateException | UncheckedIOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
