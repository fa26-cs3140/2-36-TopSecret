
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class Homework5Menu {

    private final ModelDatabase database;
    private final Scanner scanner;
    private final PrintStream out;
    private final AuditLog audit;
    private final AccountManager accounts;
    private final UserAccount user;
    private final boolean admin;

    public Homework5Menu(
            ModelDatabase database,
            Scanner scanner,
            PrintStream out,
            AuditLog audit,
            AccountManager accounts,
            UserAccount user,
            boolean admin) {

        this.database = database;
        this.scanner = scanner;
        this.out = out;
        this.audit = audit;
        this.accounts = accounts;
        this.user = user;
        this.admin = admin && user.isAdmin();
    }

    public void run() {
        while (true) {
            out.println("\n=== MAIN MENU ===");
            out.println("1. Mission Briefs");
            out.println("2. Agents");
            out.println("3. Facilities");

            if (admin) {
                out.println("4. View Audit Log");
                out.println("5. Add User");
                out.println("6. Manage User Access");
            }

            out.println("0. Back / Exit");

            String choice = prompt("Choose an option: ");
            if (choice == null) return;

            switch (choice) {
                case "1" -> recordsMenu("Briefs");
                case "2" -> recordsMenu("Agents");
                case "3" -> recordsMenu("Facilities");
                case "4" -> {
                    if (admin) viewAuditLog();
                    else out.println("Invalid choice.");
                }
                case "5" -> {
                    if (admin) addUser();
                    else out.println("Invalid choice.");
                }
                case "6" -> {
                    if (admin) manageAccess();
                    else out.println("Invalid choice.");
                }
                case "0" -> {
                    return;
                }
                default -> out.println("Invalid choice.");
            }
        }
    }

    private String prompt(String message) {
        out.print(message);
        if (!scanner.hasNextLine()) return null;
        return scanner.nextLine().trim();
    }

    private void recordsMenu(String type) {
        while (true) {
            out.println("\n=== " + type.toUpperCase() + " ===");
            out.println("1. List entries");
            out.println("2. Read an entry");

            if (admin) {
                out.println("3. Add an entry");
                out.println("4. Update an entry");
                out.println("5. Delete an entry");
            }

            out.println("0. Back");

            String choice = prompt("Choose an option: ");
            if (choice == null) return;

            try {
                switch (choice) {
                    case "1" -> listEntries(type);
                    case "2" -> readEntry(type);
                    case "3" -> {
                        if (admin) addEntry(type);
                        else out.println("Invalid choice.");
                    }
                    case "4" -> {
                        if (admin) updateEntry(type);
                        else out.println("Invalid choice.");
                    }
                    case "5" -> {
                        if (admin) deleteEntry(type);
                        else out.println("Invalid choice.");
                    }
                    case "0" -> {
                        return;
                    }
                    default -> out.println("Invalid choice.");
                }
            } catch (SQLException e) {
                out.println("Database error: " + e.getMessage());
            } catch (IllegalArgumentException | IllegalStateException e) {
                out.println("Error: " + e.getMessage());
            }
        }
    }

    private void listEntries(String type) throws SQLException {
        out.println("\n=== ALL " + type.toUpperCase() + " ===");

        switch (type) {
            case "Briefs" -> {
                List<Brief> briefs = database.getBriefs();

                if (briefs.isEmpty()) {
                    out.println("No briefs available.");
                }

                for (Brief b : briefs) {
                    out.println(b.getId() + ". " + b.getTitle());
                    auditRead("Brief #" + b.getId());
                }
            }

            case "Agents" -> {
                List<Agent> agents = database.getAgents();

                if (agents.isEmpty()) {
                    out.println("No agents available.");
                }

                for (Agent a : agents) {
                    out.println(a.getId() + ". " + a.getName());
                    auditRead("Agent #" + a.getId());
                }
            }

            case "Facilities" -> {
                List<Facility> facilities = database.getFacilities();

                if (facilities.isEmpty()) {
                    out.println("No facilities available.");
                }

                for (Facility f : facilities) {
                    out.println(f.getId() + ". "
                            + f.getName() + " ("
                            + f.getAbbreviation() + ")");
                    auditRead("Facility #" + f.getId());
                }
            }
        }
    }

    private void readEntry(String type) throws SQLException {
        String input = prompt("Enter entry ID: ");
        if (input == null) return;

        int id;
        try {
            id = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            out.println("Please enter a valid number.");
            return;
        }

        boolean found = false;

        switch (type) {
            case "Briefs" -> {
                for (Brief b : database.getBriefs()) {
                    if (b.getId() == id) {
                        out.println("Title: " + b.getTitle());
                        out.println("Date: " + b.getDate());
                        out.println("Text: " + b.getText());

                        auditRead("Brief #" + id);
                        found = true;
                        break;
                    }
                }
            }

            case "Agents" -> {
                for (Agent a : database.getAgents()) {
                    if (a.getId() == id) {
                        out.println("Name: " + a.getName());
                        out.println("Birth: " + a.getDateOfBirth());
                        out.println("Death: " + a.getDateOfDeath());
                        out.println("Notes: " + a.getNotes());
                        out.println("Facility ID: " + a.getFacilityId());

                        auditRead("Agent #" + id);
                        found = true;
                        break;
                    }
                }
            }

            case "Facilities" -> {
                for (Facility f : database.getFacilities()) {
                    if (f.getId() == id) {
                        out.println("Name: " + f.getName());
                        out.println("Abbreviation: "
                                + f.getAbbreviation());

                        auditRead("Facility #" + id);
                        found = true;
                        break;
                    }
                }
            }
        }

        if (!found) {
            out.println("Entry not found.");
        }
    }

    private void addEntry(String type) throws SQLException {
        if (!admin) {
            out.println("Admin access required.");
            return;
        }

        switch (type) {
            case "Briefs" -> addBrief();
            case "Agents" -> addAgent();
            case "Facilities" -> addFacility();
        }
    }

    private void addBrief() throws SQLException {
        out.println("\n=== ADD BRIEF ===");

        String title = prompt("Title: ");
        String date = prompt("Date: ");
        String text = prompt("Text: ");

        if (title == null || date == null || text == null) return;

        if (title.isBlank() || date.isBlank() || text.isBlank()) {
            out.println("All fields are required.");
            return;
        }

        int id = database.addBrief(title, date, text);
        auditCreate("Brief #" + id);

        out.println("Brief added successfully! ID: " + id);
        listEntries("Briefs");
    }

    private void addFacility() throws SQLException {
        out.println("\n=== ADD FACILITY ===");

        String name = prompt("Facility name: ");
        String abbreviation = prompt("Abbreviation: ");

        if (name == null || abbreviation == null) return;

        if (name.isBlank() || abbreviation.isBlank()) {
            out.println("All fields are required.");
            return;
        }

        int id = database.addFacility(name, abbreviation);
        auditCreate("Facility #" + id);

        out.println("Facility added successfully! ID: " + id);
        listEntries("Facilities");
    }

    private void addAgent() throws SQLException {
        out.println("\n=== ADD AGENT ===");

        String name = prompt("Agent name: ");
        String dob = prompt("Date of birth: ");
        String dod = prompt("Date of death (blank if alive): ");
        String notes = prompt("Notes: ");

        if (name == null || dob == null
                || dod == null || notes == null) return;

        if (name.isBlank()) {
            out.println("Agent name is required.");
            return;
        }

        List<Facility> facilities = database.getFacilities();

        if (facilities.isEmpty()) {
            out.println("Add a facility before adding an agent.");
            return;
        }

        out.println("\nAvailable facilities:");

        for (Facility f : facilities) {
            out.println(f.getId() + ". " + f.getName());
        }

        String input = prompt("Facility ID: ");
        if (input == null) return;

        int facilityId;
        try {
            facilityId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            out.println("Invalid facility ID.");
            return;
        }

        boolean exists = facilities.stream()
                .anyMatch(f -> f.getId() == facilityId);

        if (!exists) {
            out.println("Facility does not exist.");
            return;
        }

        int id = database.addAgent(
                name, dob, dod, notes, facilityId);

        auditCreate("Agent #" + id);

        out.println("Agent added successfully! ID: " + id);
        listEntries("Agents");
    }

    // HOMEWORK 5: UPDATE ENTRY FEATURE

    private void updateEntry(String type) throws SQLException {
        if (!admin) {
            out.println("Admin access required.");
            return;
        }

        String input = prompt("Enter entry ID to update: ");
        if (input == null) return;

        int id;
        try {
            id = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            out.println("Invalid ID.");
            return;
        }

        switch (type) {
            case "Briefs" -> updateBrief(id);
            case "Agents" -> updateAgent(id);
            case "Facilities" -> updateFacility(id);
        }
    }

    private void updateBrief(int id) throws SQLException {
        boolean exists = database.getBriefs().stream()
                .anyMatch(b -> b.getId() == id);

        if (!exists) {
            out.println("Brief not found.");
            return;
        }

        out.println("\n=== UPDATE BRIEF ===");

        String title = prompt("New title: ");
        String date = prompt("New date: ");
        String text = prompt("New text: ");

        if (title == null || date == null || text == null) return;

        if (title.isBlank() || date.isBlank() || text.isBlank()) {
            out.println("All fields are required.");
            return;
        }

        boolean updated = database.updateBrief(
                id, title, date, text);

        if (updated) {
            auditUpdate("Brief #" + id);
            out.println("Brief updated successfully!");
            listEntries("Briefs");
        } else {
            out.println("Brief not found.");
        }
    }

    private void updateFacility(int id) throws SQLException {
        boolean exists = database.getFacilities().stream()
                .anyMatch(f -> f.getId() == id);

        if (!exists) {
            out.println("Facility not found.");
            return;
        }

        out.println("\n=== UPDATE FACILITY ===");

        String name = prompt("New facility name: ");
        String abbreviation = prompt("New abbreviation: ");

        if (name == null || abbreviation == null) return;

        if (name.isBlank() || abbreviation.isBlank()) {
            out.println("All fields are required.");
            return;
        }

        boolean updated = database.updateFacility(
                id, name, abbreviation);

        if (updated) {
            auditUpdate("Facility #" + id);
            out.println("Facility updated successfully!");
            listEntries("Facilities");
        } else {
            out.println("Facility not found.");
        }
    }

    private void updateAgent(int id) throws SQLException {
        boolean exists = database.getAgents().stream()
                .anyMatch(a -> a.getId() == id);

        if (!exists) {
            out.println("Agent not found.");
            return;
        }

        out.println("\n=== UPDATE AGENT ===");

        String name = prompt("New agent name: ");
        String dob = prompt("New date of birth: ");
        String dod = prompt("New date of death (blank if alive): ");
        String notes = prompt("New notes: ");

        if (name == null || dob == null
                || dod == null || notes == null) return;

        if (name.isBlank()) {
            out.println("Name is required.");
            return;
        }

        List<Facility> facilities = database.getFacilities();

        out.println("\nAvailable facilities:");
        for (Facility f : facilities) {
            out.println(f.getId() + ". " + f.getName());
        }

        String facilityInput = prompt("New facility ID: ");
        if (facilityInput == null) return;

        int facilityId;
        try {
            facilityId = Integer.parseInt(facilityInput);
        } catch (NumberFormatException e) {
            out.println("Invalid facility ID.");
            return;
        }

        boolean facilityExists = facilities.stream()
                .anyMatch(f -> f.getId() == facilityId);

        if (!facilityExists) {
            out.println("Facility does not exist.");
            return;
        }

        boolean updated = database.updateAgent(
                id, name, dob, dod, notes, facilityId);

        if (updated) {
            auditUpdate("Agent #" + id);
            out.println("Agent updated successfully!");
            listEntries("Agents");
        } else {
            out.println("Agent not found.");
        }
    }

    private void auditRead(String item) {
        audit.record(user.getUsername(), AuditLog.READ, item);
    }

    private void auditCreate(String item) {
        audit.record(user.getUsername(), AuditLog.CREATE, item);
    }

    private void auditUpdate(String item) {
        audit.record(user.getUsername(), AuditLog.UPDATE, item);
    }

    // HOMEWORK 5: DELETE ENTRY (the database refuses deletes that would leave orphaned data)

    private void deleteEntry(String type) throws SQLException {
        String input = prompt("Enter entry ID to delete: ");
        if (input == null) return;

        int id;
        try {
            id = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            out.println("Invalid ID.");
            return;
        }

        String item = switch (type) {
            case "Briefs" -> "Brief #" + id;
            case "Agents" -> "Agent #" + id;
            default -> "Facility #" + id;
        };
        boolean deleted = switch (type) {
            case "Briefs" -> database.deleteBrief(id);
            case "Agents" -> database.deleteAgent(id);
            default -> database.deleteFacility(id);
        };

        if (deleted) {
            audit.record(user.getUsername(), AuditLog.DELETE, item);
            out.println(item + " deleted.");
        } else {
            out.println("Entry not found.");
        }
    }

    // HOMEWORK 5: ACCESS LEVELS (promote / demote, admins only, not yourself)

    private void manageAccess() {
        out.println("\n=== USER ACCESS ===");
        for (UserAccount u : accounts.getUsers()) {
            out.println(u.getUsername() + " - " + (u.isAdmin() ? "admin" : "standard"));
        }

        String username = prompt("Username to change: ");
        if (username == null) return;
        String level = prompt("1. Make admin  2. Make standard user: ");
        if (level == null) return;

        if (!level.equals("1") && !level.equals("2")) {
            out.println("Invalid choice.");
            return;
        }
        boolean makeAdmin = level.equals("1");

        try {
            accounts.setAdmin(user, username, makeAdmin);
            audit.record(user.getUsername(), AuditLog.UPDATE,
                    "User access: " + username + " -> " + (makeAdmin ? "admin" : "standard"));
            out.println(username + " is now " + (makeAdmin ? "an admin." : "a standard user."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            out.println("Error: " + e.getMessage());
        }
    }

    private void viewAuditLog() {
        if (!admin) {
            out.println("Admin access required.");
            return;
        }

        out.println("\n=== AUDIT LOG ===");

        List<AuditEntry> entries = audit.getEntries();

        if (entries.isEmpty()) {
            out.println("No audit entries available.");
            return;
        }

        for (AuditEntry entry : entries) {
            out.println(entry);
        }
    }

    private void addUser() {
        if (!admin) {
            out.println("Admin access required.");
            return;
        }

        String username = prompt("New username: ");
        if (username == null) return;

        try {
            accounts.addUser(user, username);
            auditCreate("User account: " + username);
            out.println("User created successfully.");
            out.println("They can set a password at first login.");
        } catch (IllegalArgumentException |
                 IllegalStateException e) {
            out.println("Error: " + e.getMessage());
        }
    }
}
