package view;

import model.AuditEntry;
import model.AuditLog;
import model.Mission;
import model.MissionStore;
import model.UserAccount;
import controller.PageResult;
import controller.WebController;
import model.Message;
import model.MessageStore;
import model.ModelDatabase;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class WebPages implements WebController.Pages {

    private final Connection connection;
    private final MissionStore missions;
    private final AuditLog audit;
    private final MessageStore messages;

    private final ModelDatabase database;
    private final RecordPages records;

    private final HtmlView view = new HtmlView();


    public WebPages(
            Connection connection,
            MissionStore missions,
            AuditLog audit) throws SQLException {

        this(connection, missions, audit, null);
    }

    public WebPages(
            Connection connection,
            MissionStore missions,
            AuditLog audit,
            MessageStore messages) throws SQLException {

        this.connection = Objects.requireNonNull(connection);
        this.missions = Objects.requireNonNull(missions);
        this.audit = Objects.requireNonNull(audit);
        this.messages = messages;
        this.database = new ModelDatabase(connection);
        this.records = new RecordPages(database, audit);
    }

    // HW7: one record's page and the add/edit forms. Null for any other address.
    @Override
    public synchronized PageResult handle(
            String method, String path,
            Map<String, String> params, UserAccount user) throws SQLException {

        return records.handle(method, path, params, user);
    }

    @Override
    public synchronized String render(
            String path, UserAccount user) throws SQLException {

        if (path.equals("/")) {
            return view.renderMessage(
                    "Welcome, " + user.getUsername());
        }
        if (path.equals("/messages")) {
            return renderInbox(user);
        }

        if (path.equals("/messages/new")) {
            return renderComposeMessage();
        }

        if (path.equals("/audit")) {
            if (!user.isAdmin()) {
                throw new IllegalStateException(
                        "Administrator access required.");
            }

            audit.record(
                    user.getUsername(),
                    AuditLog.READ,
                    "Web audit log");

            return view.renderAuditLog(
                    audit.getEntries()
                            .stream()
                            .map(AuditEntry::toString)
                            .collect(Collectors.toList()));
        }

        String html;

        switch (path) {
            case "/agents":
                html = view.renderLinkedTable(
                        List.of("ID", "Name", "Date of Birth",
                                "Date of Death", "Notes", "Facility"),
                        rows("SELECT a.id,a.name,a.date_of_birth,a.date_of_death,"
                                + "a.notes,f.name FROM agents a "
                                + "LEFT JOIN facilities f ON f.id = a.facility_id "
                                + "ORDER BY a.id"),
                        path);
                break;

            case "/facilities":
                html = view.renderLinkedTable(
                        List.of("ID", "Name", "Abbreviation", "Country"),
                        rows("SELECT f.id,f.name,f.abbreviation,c.name "
                                + "FROM facilities f "
                                + "LEFT JOIN countries c ON c.id = f.country_id "
                                + "ORDER BY f.id"),
                        path);
                break;

            case "/briefs":
                html = view.renderLinkedTable(
                        List.of("ID", "Title", "Date", "Text"),
                        rows("SELECT id,title,date,text "
                                + "FROM briefs ORDER BY id"),
                        path);
                break;

            case "/countries":
                html = CountryViews.countryListBody(database.getCountries());
                break;

            case "/missions":
                List<List<String>> data = new ArrayList<>();

                for (Mission mission : missionsWithLinks()) {
                    String agentIds = mission.getAgentIds()
                            .stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", "));

                    data.add(Arrays.asList(
                            String.valueOf(mission.getId()),
                            mission.getTitle(),
                            mission.getBrief(),
                            mission.getDate(),
                            String.valueOf(mission.getFacilityId()),
                            agentIds
                    ));
                }

                html = view.renderLinkedTable(
                        List.of("ID", "Title", "Brief", "Date",
                                "Facility ID", "Agent IDs"),
                        data, path);
                break;

            default:
                throw new IllegalArgumentException("Unknown page.");
        }

        audit.record(
                user.getUsername(),
                AuditLog.READ,
                "Web " + path.substring(1));

        // Admins can add a record from each list page
        if (user.isAdmin()) {
            html += "<p><a href=\"" + path + "/new\">Add new</a></p>\n";
        }

        return html;
    }

    // Missions with their facility and agents. The mission store only knows the
    // mission text, so the assignments come from the database when it has them.
    private List<Mission> missionsWithLinks() {
        try {
            return database.getMissions();
        } catch (SQLException e) {
            return missions.getAllMissions();
        }
    }

    // HW7 Part C: Display the logged-in agent's inbox.
    public String renderInbox(UserAccount user) {
        if (messages == null) {
            return view.renderMessage("Messaging is not configured.");
        }

        return view.renderInbox(
                messages.getMessagesForExactRecipient(user.getUsername()));
    }

    // HW7 Part C: Display the compose-message form.
    public synchronized String renderComposeMessage()
            throws SQLException {

        List<String> agentNames = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT name FROM agents ORDER BY name")) {

            while (rs.next()) {
                agentNames.add(rs.getString("name"));
            }
        }

        return view.renderComposeMessage(agentNames);
    }

    // HW7 Part C: Display a message and mark it as read.
    public String renderMessageDetails(int messageId, UserAccount user) {
        if (messages == null) {
            return view.renderMessage("Messaging is not configured.");
        }

        String username = user.getUsername();

        Message message = messages.getMessage(messageId, username);

        if (message == null) {
            return view.renderMessage("Message not found.");
        }

        messages.markAsRead(messageId, username);

        return view.renderMessageDetails(message);
    }

    // HW7 Part C: Send a message.
    public void sendMessage(
            UserAccount user, String agentName, String text) {

        if (messages == null) {
            throw new IllegalStateException(
                    "Messaging is not configured.");
        }

        messages.send(user.getUsername(), agentName, text);
    }

    private List<List<String>> rows(String sql) throws SQLException {
        List<List<String>> result = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet records = statement.executeQuery(sql)) {

            int columns = records.getMetaData().getColumnCount();

            while (records.next()) {
                List<String> row = new ArrayList<>();

                for (int i = 1; i <= columns; i++) {
                    row.add(Objects.toString(
                            records.getString(i), ""));
                }

                result.add(row);
            }
        }

        return result;
    }
}
