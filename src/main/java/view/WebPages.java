package view;

import model.AuditEntry;
import model.AuditLog;
import model.Mission;
import model.MissionStore;
import model.UserAccount;
import controller.WebController;
import view.HtmlView;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class WebPages implements WebController.Pages {

    private final Connection connection;
    private final MissionStore missions;
    private final AuditLog audit;

    private final HtmlView view = new HtmlView();

    public WebPages(
            Connection connection,
            MissionStore missions,
            AuditLog audit) {

        this.connection = Objects.requireNonNull(connection);
        this.missions = Objects.requireNonNull(missions);
        this.audit = Objects.requireNonNull(audit);
    }

    @Override
    public synchronized String render(
            String path, UserAccount user) throws SQLException {

        if (path.equals("/")) {
            return view.renderMessage(
                    "Welcome, " + user.getUsername());
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
                html = view.renderAgents(rows(
                        "SELECT id,name,date_of_birth,date_of_death,"
                                + "notes,facility_id FROM agents ORDER BY id"));
                break;

            case "/facilities":
                html = view.renderFacilities(rows(
                        "SELECT id,name,abbreviation "
                                + "FROM facilities ORDER BY id"));
                break;

            case "/briefs":
                html = view.renderBriefs(rows(
                        "SELECT id,title,date,text "
                                + "FROM briefs ORDER BY id"));
                break;

            case "/missions":
                List<List<String>> data = new ArrayList<>();

                for (Mission mission : missions.getAllMissions()) {
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

                html = view.renderMissions(data);
                break;

            default:
                throw new IllegalArgumentException("Unknown page.");
        }

        audit.record(
                user.getUsername(),
                AuditLog.READ,
                "Web " + path.substring(1));

        return html;
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