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

    public MissionMenu(MissionRepository missions, MissionSearch search, Scanner in, PrintStream out) {
        this.missions = missions;
        this.search = search;
        this.in = in;
        this.out = out;
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