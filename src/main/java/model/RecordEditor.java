package model;

import java.sql.SQLException;

// Creates and updates records for the other roles (Member D).
// Every change made here is written to the audit log, so edits from the
// web view are recorded the same way as edits from the terminal.
// In each save method an id of 0 creates a new record and any other id
// updates that record. The id of the saved record is returned.
public class RecordEditor {

    private final ModelDatabase database;
    private final AuditLog audit;

    public RecordEditor(ModelDatabase database, AuditLog audit) {
        this.database = database;
        this.audit = audit;
    }

    public int saveBrief(String username, int id, String title, String date,
                         String text) throws SQLException {
        title = required(title, "Title");
        date = required(date, "Date");
        text = required(text, "Text");
        if (id == 0) {
            return created(username, "Brief", database.addBrief(title, date, text));
        }
        return updated(username, "Brief", id, database.updateBrief(id, title, date, text));
    }

    public int saveAgent(String username, int id, String name, String dateOfBirth,
                         String dateOfDeath, String notes, int facilityId) throws SQLException {
        name = required(name, "Name");
        dateOfBirth = optional(dateOfBirth);
        dateOfDeath = optional(dateOfDeath);
        notes = optional(notes);
        if (facilityId <= 0) {
            throw new IllegalArgumentException("Facility is required");
        }
        if (id == 0) {
            return created(username, "Agent",
                    database.addAgent(name, dateOfBirth, dateOfDeath, notes, facilityId));
        }
        return updated(username, "Agent", id,
                database.updateAgent(id, name, dateOfBirth, dateOfDeath, notes, facilityId));
    }

    // A countryId of 0 means the facility has no country
    public int saveFacility(String username, int id, String name, String abbreviation,
                            int countryId) throws SQLException {
        name = required(name, "Name");
        abbreviation = required(abbreviation, "Abbreviation");
        if (id == 0) {
            int newId = database.addFacility(name, abbreviation);
            database.setFacilityCountry(newId, countryId);
            return created(username, "Facility", newId);
        }
        boolean found = database.updateFacility(id, name, abbreviation);
        if (found) {
            database.setFacilityCountry(id, countryId);
        }
        return updated(username, "Facility", id, found);
    }

    public int saveCountry(String username, int id, String name) throws SQLException {
        name = required(name, "Name");
        if (id == 0) {
            return created(username, "Country", database.addCountry(name));
        }
        return updated(username, "Country", id, database.updateCountry(id, name));
    }

    public int saveMission(String username, int id, String title, String brief, String date,
                           int facilityId) throws SQLException {
        title = required(title, "Title");
        brief = required(brief, "Brief");
        date = required(date, "Date");
        if (facilityId <= 0) {
            throw new IllegalArgumentException("Facility is required");
        }
        if (id == 0) {
            return created(username, "Mission",
                    database.addMission(title, brief, date, facilityId));
        }
        return updated(username, "Mission", id,
                database.updateMission(id, title, brief, date, facilityId));
    }

    private int created(String username, String type, int id) {
        audit.record(username, AuditLog.CREATE, type + " #" + id);
        return id;
    }

    private int updated(String username, String type, int id, boolean found) {
        if (!found) {
            throw new IllegalArgumentException("No such " + type.toLowerCase() + ": " + id);
        }
        audit.record(username, AuditLog.UPDATE, type + " #" + id);
        return id;
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    // Blank optional fields are stored as empty (NULL)
    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
