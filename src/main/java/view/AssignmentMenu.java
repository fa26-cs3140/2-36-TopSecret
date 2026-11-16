package view;

import model.Agent;
import model.AuditLog;
import model.Brief;
import model.Facility;
import model.Mission;
import model.ModelDatabase;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

// Terminal menu: create missions, assign agents to them
// (joint operations: agents may come from any facility), and attach briefs.
// Modeled on AccessMenu. Changes are recorded in the audit log when one is given.
public class AssignmentMenu {

    private final ModelDatabase db;
    private final Scanner in;
    private final PrintStream out;
    private final AuditLog audit; // may be null

    public AssignmentMenu(ModelDatabase db, Scanner in, PrintStream out, AuditLog audit) {
        this.db = db;
        this.in = in;
        this.out = out;
        this.audit = audit;
    }

    public void run(String actingUser) {
        while (true) {
            showMenu();
            if (!in.hasNextLine()) {
                return;
            }
            String choice = in.nextLine().trim();
            if (choice.equals("6")) {
                return;
            }
            try {
                switch (choice) {
                    case "1" -> listMissions();
                    case "2" -> createMission(actingUser);
                    case "3" -> assignAgent(actingUser);
                    case "4" -> removeAgent(actingUser);
                    case "5" -> attachBrief(actingUser);
                    default -> out.println("Invalid choice. Enter 1, 2, 3, 4, 5, or 6.");
                }
            } catch (SQLException | IllegalArgumentException e) {
                out.println("Error: " + e.getMessage());
            }
        }
    }

    void showMenu() {
        out.println();
        out.println("1. List missions");
        out.println("2. Create a mission");
        out.println("3. Assign an agent to a mission");
        out.println("4. Remove an agent from a mission");
        out.println("5. Attach a brief to a mission");
        out.println("6. Back");
        out.print("Choose an option: ");
    }

    void listMissions() throws SQLException {
        List<Mission> missions = db.getMissions();
        if (missions.isEmpty()) {
            out.println("No missions.");
            return;
        }
        Map<Integer, String> agentNames = new HashMap<>();
        for (Agent a : db.getAgents()) {
            agentNames.put(a.getId(), a.getName());
        }
        Map<Integer, String> facilityNames = new HashMap<>();
        for (Facility f : db.getFacilities()) {
            facilityNames.put(f.getId(), f.getName());
        }
        for (Mission m : missions) {
            out.println("Mission #" + m.getId() + ": " + m.getTitle() + " (" + m.getDate() + ")");
            out.println("  Facility: " + facilityNames.getOrDefault(m.getFacilityId(), "none"));
            List<String> names = new ArrayList<>();
            for (int agentId : m.getAgentIds()) {
                names.add(agentNames.getOrDefault(agentId, "#" + agentId));
            }
            out.println("  Agents: " + (names.isEmpty() ? "none" : String.join(", ", names)));
            out.println("  Briefs: " + m.getBriefIds().size());
        }
    }

    void createMission(String actingUser) throws SQLException {
        String title = ask("Title: ");
        String date = ask("Date (YYYY-MM-DD): ");
        String brief = ask("Brief text (optional): ");
        if (title == null || date == null || brief == null) {
            return;
        }
        listFacilities();
        Integer facilityId = askInt("Facility id (runs the mission): ");
        if (facilityId == null) {
            return;
        }
        int id = db.addMission(title, brief, date, facilityId);
        out.println("Mission #" + id + " created.");
        log(AuditLog.CREATE, actingUser, "Mission #" + id + ": " + title.trim());
    }

    void assignAgent(String actingUser) throws SQLException {
        listAgents();
        Integer missionId = askInt("Mission id: ");
        if (missionId == null) {
            return;
        }
        Integer agentId = askInt("Agent id: ");
        if (agentId == null) {
            return;
        }
        if (db.assignAgent(missionId, agentId)) {
            out.println("Agent #" + agentId + " assigned to mission #" + missionId + ".");
            log(AuditLog.CREATE, actingUser,
                    "Assignment: agent #" + agentId + " to mission #" + missionId);
        } else {
            out.println("Agent #" + agentId + " is already assigned to mission #" + missionId + ".");
        }
    }

    void removeAgent(String actingUser) throws SQLException {
        Integer missionId = askInt("Mission id: ");
        if (missionId == null) {
            return;
        }
        Integer agentId = askInt("Agent id: ");
        if (agentId == null) {
            return;
        }
        if (db.unassignAgent(missionId, agentId)) {
            out.println("Agent #" + agentId + " removed from mission #" + missionId + ".");
            log("DELETE", actingUser,
                    "Assignment removed: agent #" + agentId + " from mission #" + missionId);
        } else {
            out.println("Agent #" + agentId + " is not assigned to mission #" + missionId + ".");
        }
    }

    void attachBrief(String actingUser) throws SQLException {
        for (Brief b : db.getBriefs()) {
            out.println("  " + b.getId() + ": " + b.getTitle());
        }
        Integer missionId = askInt("Mission id: ");
        if (missionId == null) {
            return;
        }
        Integer briefId = askInt("Brief id: ");
        if (briefId == null) {
            return;
        }
        if (db.attachBrief(missionId, briefId)) {
            out.println("Brief #" + briefId + " attached to mission #" + missionId + ".");
            log(AuditLog.CREATE, actingUser,
                    "Brief #" + briefId + " attached to mission #" + missionId);
        } else {
            out.println("Brief #" + briefId + " is already attached to mission #" + missionId + ".");
        }
    }

    private void listFacilities() throws SQLException {
        for (Facility f : db.getFacilities()) {
            out.println("  " + f.getId() + ": " + f.getName() + " (" + f.getAbbreviation() + ")");
        }
    }

    private void listAgents() throws SQLException {
        for (Agent a : db.getAgents()) {
            out.println("  " + a.getId() + ": " + a.getName() + " (facility " + a.getFacilityId() + ")");
        }
    }

    // Records an event in the audit log, if this menu has one
    private void log(String action, String user, String item) {
        if (audit != null && user != null) {
            audit.record(user, action, item);
        }
    }

    // Prints the prompt and reads a line, or returns null if there is no more input
    private String ask(String prompt) {
        out.print(prompt);
        return in.hasNextLine() ? in.nextLine() : null;
    }

    // Reads a whole number, or prints an error and returns null
    private Integer askInt(String prompt) {
        String input = ask(prompt);
        if (input == null) {
            return null;
        }
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            out.println("Error: '" + input.trim() + "' is not a number.");
            return null;
        }
    }
}