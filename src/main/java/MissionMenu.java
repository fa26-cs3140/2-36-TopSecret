import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

// Interactive menu shown after the user logs in (Member D).
// Keeps running until the user chooses Exit.
public class MissionMenu {

    private final MissionRepository missions;
    private final MissionSearch search;
    private final Scanner in;
    private final PrintStream out;
    private final AuditLog audit;
    private final AccountManager accounts;
    private final UserAccount user;

    // Homework 4 menu: no audit log and no account options
    public MissionMenu(MissionRepository missions, MissionSearch search, Scanner in, PrintStream out) {
        this(missions, search, in, out, null, null, null);
    }

    // Homework 5 menu: records reads and creates in the audit log for the logged-in user
    public MissionMenu(MissionRepository missions, MissionSearch search, Scanner in, PrintStream out,
                       AuditLog audit, AccountManager accounts, UserAccount user) {
        this.missions = missions;
        this.search = search;
        this.in = in;
        this.out = out;
        this.audit = audit;
        this.accounts = accounts;
        this.user = user;
    }

    public void run() {
        while (true) {
            showMenu();
            if (!in.hasNextLine()) {
                return; // no more input
            }
            String choice = in.nextLine().trim();
            switch (choice) {
                case "1" -> listMissions();
                case "2" -> readMission();
                case "3" -> searchMissions();
                case "4" -> {
                    out.println("Goodbye.");
                    return;
                }
                case "5" -> {
                    if (canAddUsers()) {
                        addUser();
                    } else {
                        out.println("Invalid choice. Enter 1, 2, 3, or 4.");
                    }
                }
                default -> out.println("Invalid choice. Enter 1, 2, 3, or 4.");
            }
        }
    }

    void showMenu() {
        out.println();
        out.println("1. List missions");
        out.println("2. Read a mission");
        out.println("3. Search missions");
        out.println("4. Exit");
        if (canAddUsers()) {
            out.println("5. Add user");
        }
        out.print("Choose an option: ");
    }

    void listMissions() {
        List<Mission> all = missions.getAllMissions();
        if (all.isEmpty()) {
            out.println("No missions available.");
            return;
        }
        for (int i = 0; i < all.size(); i++) {
            out.println((i + 1) + ". " + all.get(i).getTitle());
        }
    }

    void readMission() {
        out.print("Enter mission number: ");
        if (!in.hasNextLine()) {
            return;
        }
        String input = in.nextLine().trim();
        List<Mission> all = missions.getAllMissions();
        int number;
        try {
            number = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            out.println("Error: '" + input + "' is not a number.");
            return;
        }
        if (number < 1 || number > all.size()) {
            out.println("Error: No mission with number " + number + ".");
            return;
        }
        Mission m = all.get(number - 1);
        out.println(m.getTitle() + " (" + m.getDate() + ")");
        out.println(m.getBrief());
        logEvent(AuditLog.READ, "Mission brief #" + m.getId() + ": " + m.getTitle());
    }

    void addUser() {
        out.print("New username: ");
        if (!in.hasNextLine()) {
            return;
        }
        String name = in.nextLine().trim();
        try {
            accounts.addUser(user, name);
            out.println("User '" + name + "' added. They will create a password at first login.");
            logEvent(AuditLog.CREATE, "User account: " + name);
        } catch (IllegalArgumentException | IllegalStateException e) {
            out.println("Error: " + e.getMessage());
        }
    }

    // Only logged-in admins see and can use "Add user"
    private boolean canAddUsers() {
        return accounts != null && user != null && user.isAdmin();
    }

    // Records an event in the audit log, if this menu has one
    private void logEvent(String action, String item) {
        if (audit != null && user != null) {
            audit.record(user.getUsername(), action, item);
        }
    }

    void searchMissions() {
        out.print("Enter a word or phrase: ");
        if (!in.hasNextLine()) {
            return;
        }
        String query = in.nextLine();
        List<Mission> matches = search.searchBriefs(query);
        if (matches.isEmpty()) {
            out.println("No matches found.");
            return;
        }
        for (Mission m : matches) {
            out.println("- " + m.getTitle());
        }
    }
}