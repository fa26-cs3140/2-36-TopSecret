package view;

import controller.PageResult;
import model.Agent;
import model.AuditLog;
import model.Brief;
import model.Country;
import model.Facility;
import model.Mission;
import model.ModelDatabase;
import model.RecordEditor;
import model.RelatedRecords;
import model.UserAccount;
import view.RecordViews.Input;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// The record pages of the web view (HW7):
//   GET  /agents/7            details, with clickable related records
//   GET  /agents/new          empty form            (admins only)
//   GET  /agents/edit?id=7    form filled in        (admins only)
//   POST to either form       saves, then shows the saved record
// The same addresses work for facilities, briefs, missions and countries.
// Reads and changes are written to the audit log.
public class RecordPages {

    private static final String TYPES = "(agents|facilities|briefs|missions|countries)";
    private static final Pattern DETAIL = Pattern.compile("/" + TYPES + "/([0-9]{1,9})");
    private static final Pattern FORM = Pattern.compile("/" + TYPES + "/(new|edit)");

    private final ModelDatabase database;
    private final RelatedRecords related;
    private final RecordEditor editor;
    private final AuditLog audit;
    private final RecordViews views = new RecordViews();
    private final HtmlView html = new HtmlView();

    public RecordPages(ModelDatabase database, AuditLog audit) {
        this.database = database;
        this.related = new RelatedRecords(database);
        this.editor = new RecordEditor(database, audit);
        this.audit = audit;
    }

    // True for addresses that show or save an add/edit form
    public static boolean isForm(String path) {
        return FORM.matcher(path).matches();
    }

    // True for the address of one record, such as /agents/7
    public static boolean isDetail(String path) {
        return DETAIL.matcher(path).matches();
    }

    // Returns null when the address is not a record page
    public PageResult handle(String method, String path, Map<String, String> params,
                             UserAccount user) throws SQLException {
        Matcher detail = DETAIL.matcher(path);
        if (detail.matches()) {
            return showRecord(detail.group(1), Integer.parseInt(detail.group(2)), user);
        }
        Matcher form = FORM.matcher(path);
        if (!form.matches()) {
            return null;
        }
        if (!user.isAdmin()) {
            return PageResult.error(403, "Not allowed",
                    views.error("Administrator access required."));
        }
        String type = form.group(1);
        int id = form.group(2).equals("new") ? 0 : number(params.get("id"));
        if (form.group(2).equals("edit") && id <= 0) {
            return notFound();
        }
        if (method.equals("POST")) {
            return save(type, id, params, user);
        }
        Map<String, String> values = id == 0 ? Map.of() : currentValues(type, id);
        return values == null ? notFound() : showForm(200, type, id, values, null);
    }

    // ---------- Details with related records ----------

    private PageResult showRecord(String type, int id, UserAccount user) throws SQLException {
        String title;
        String body;
        switch (type) {
            case "agents" -> {
                Agent a = related.getAgent(id);
                if (a == null) {
                    return notFound();
                }
                Facility f = related.getFacility(a.getFacilityId());
                Map<String, String> rows = new LinkedHashMap<>();
                rows.put("Name", views.text(a.getName()));
                rows.put("Date of birth", views.text(a.getDateOfBirth()));
                rows.put("Date of death", views.text(a.getDateOfDeath()));
                rows.put("Notes", views.text(a.getNotes()));
                rows.put("Facility", f == null ? views.text(null) : facilityLink(f));
                title = a.getName();
                body = views.details(rows)
                        + views.related("Missions", missionLinks(database.getMissionsForAgent(id)));
            }
            case "facilities" -> {
                Facility f = related.getFacility(id);
                if (f == null) {
                    return notFound();
                }
                title = "Facility";
                body = CountryViews.facilityBody(f, database.getCountryForFacility(id))
                        + views.related("Agents", agentLinks(related.getAgentsForFacility(id)))
                        + views.related("Missions",
                        missionLinks(related.getMissionsForFacility(id)));
            }
            case "briefs" -> {
                Brief b = related.getBrief(id);
                if (b == null) {
                    return notFound();
                }
                Mission m = related.getMissionForBrief(id);
                Map<String, String> rows = new LinkedHashMap<>();
                rows.put("Title", views.text(b.getTitle()));
                rows.put("Date", views.text(b.getDate()));
                rows.put("Mission", m == null ? views.text(null) : missionLink(m));
                title = b.getTitle();
                body = views.details(rows) + html.renderText(b.getText());
            }
            case "missions" -> {
                Mission m = findMission(id);
                if (m == null) {
                    return notFound();
                }
                Facility f = related.getFacility(m.getFacilityId());
                Map<String, String> rows = new LinkedHashMap<>();
                rows.put("Title", views.text(m.getTitle()));
                rows.put("Date", views.text(m.getDate()));
                rows.put("Facility", f == null ? views.text(null) : facilityLink(f));
                List<String> briefs = new ArrayList<>();
                for (Brief b : database.getBriefsForMission(id)) {
                    briefs.add(views.link("/briefs/" + b.getId(), b.getTitle()));
                }
                title = m.getTitle();
                body = views.details(rows) + html.renderText(m.getBrief())
                        + views.related("Agents", agentLinks(database.getAgentsForMission(id)))
                        + views.related("Briefs", briefs);
            }
            default -> {
                Country c = database.getCountry(id);
                if (c == null) {
                    return notFound();
                }
                List<String> facilities = new ArrayList<>();
                for (Facility f : related.getFacilitiesForCountry(id)) {
                    facilities.add(facilityLink(f));
                }
                title = c.getName();
                body = views.details(Map.of("Name", views.text(c.getName())))
                        + views.related("Facilities", facilities);
            }
        }
        audit.record(user.getUsername(), AuditLog.READ, label(type) + " #" + id);
        if (user.isAdmin()) {
            body += "<p>" + views.link("/" + type + "/edit?id=" + id, "Edit") + "</p>\n";
        }
        return PageResult.page(title, body);
    }

    private Mission findMission(int id) throws SQLException {
        for (Mission m : database.getMissions()) {
            if (m.getId() == id) {
                return m;
            }
        }
        return null;
    }

    private String facilityLink(Facility f) {
        return views.link("/facilities/" + f.getId(), f.getName());
    }

    private String missionLink(Mission m) {
        return views.link("/missions/" + m.getId(), m.getTitle());
    }

    private List<String> missionLinks(List<Mission> missions) {
        List<String> links = new ArrayList<>();
        for (Mission m : missions) {
            links.add(missionLink(m));
        }
        return links;
    }

    private List<String> agentLinks(List<Agent> agents) {
        List<String> links = new ArrayList<>();
        for (Agent a : agents) {
            links.add(views.link("/agents/" + a.getId(), a.getName()));
        }
        return links;
    }

    // ---------- Add and edit forms ----------

    // The stored values of a record as form fields, or null if there is no such record
    private Map<String, String> currentValues(String type, int id) throws SQLException {
        Map<String, String> values = new LinkedHashMap<>();
        switch (type) {
            case "agents" -> {
                Agent a = related.getAgent(id);
                if (a == null) {
                    return null;
                }
                values.put("name", a.getName());
                values.put("dateOfBirth", a.getDateOfBirth());
                values.put("dateOfDeath", a.getDateOfDeath());
                values.put("notes", a.getNotes());
                values.put("facilityId", String.valueOf(a.getFacilityId()));
            }
            case "facilities" -> {
                Facility f = related.getFacility(id);
                if (f == null) {
                    return null;
                }
                values.put("name", f.getName());
                values.put("abbreviation", f.getAbbreviation());
                values.put("countryId", f.getCountryId() == 0 ? "" : String.valueOf(f.getCountryId()));
            }
            case "briefs" -> {
                Brief b = related.getBrief(id);
                if (b == null) {
                    return null;
                }
                values.put("title", b.getTitle());
                values.put("date", b.getDate());
                values.put("text", b.getText());
            }
            case "missions" -> {
                Mission m = findMission(id);
                if (m == null) {
                    return null;
                }
                values.put("title", m.getTitle());
                values.put("date", m.getDate());
                values.put("brief", m.getBrief());
                values.put("facilityId", String.valueOf(m.getFacilityId()));
            }
            default -> {
                Country c = database.getCountry(id);
                if (c == null) {
                    return null;
                }
                values.put("name", c.getName());
            }
        }
        return values;
    }

    private PageResult showForm(int status, String type, int id, Map<String, String> values,
                                String error) throws SQLException {
        String action = id == 0 ? "/" + type + "/new" : "/" + type + "/edit?id=" + id;
        String form = switch (type) {
            case "agents" -> views.form(action, List.of(
                    Input.text("name", "Name", value(values, "name"), true),
                    Input.text("dateOfBirth", "Date of birth", value(values, "dateOfBirth"), false),
                    Input.text("dateOfDeath", "Date of death", value(values, "dateOfDeath"), false),
                    Input.area("notes", "Notes", value(values, "notes"), false),
                    Input.select("facilityId", "Facility", value(values, "facilityId"), true,
                            facilityOptions())));
            case "facilities" -> html.renderFacilityForm(action, value(values, "name"),
                    value(values, "abbreviation"), value(values, "countryId"), countryOptions());
            case "briefs" -> views.form(action, List.of(
                    Input.text("title", "Title", value(values, "title"), true),
                    Input.text("date", "Date", value(values, "date"), true),
                    Input.area("text", "Text", value(values, "text"), true)));
            case "missions" -> views.form(action, List.of(
                    Input.text("title", "Title", value(values, "title"), true),
                    Input.text("date", "Date", value(values, "date"), true),
                    Input.area("brief", "Brief", value(values, "brief"), true),
                    Input.select("facilityId", "Facility", value(values, "facilityId"), true,
                            facilityOptions())));
            default -> views.form(action, List.of(
                    Input.text("name", "Name", value(values, "name"), true)));
        };
        String title = (id == 0 ? "New " : "Edit ") + label(type).toLowerCase();
        return PageResult.error(status, title, views.error(error) + form);
    }

    private PageResult save(String type, int id, Map<String, String> p, UserAccount user)
            throws SQLException {
        String who = user.getUsername();
        try {
            int saved = switch (type) {
                case "agents" -> editor.saveAgent(who, id, p.get("name"), p.get("dateOfBirth"),
                        p.get("dateOfDeath"), p.get("notes"), number(p.get("facilityId")));
                case "facilities" -> editor.saveFacility(who, id, p.get("name"),
                        p.get("abbreviation"), number(p.get("countryId")));
                case "briefs" -> editor.saveBrief(who, id, p.get("title"), p.get("date"),
                        p.get("text"));
                case "missions" -> editor.saveMission(who, id, p.get("title"), p.get("brief"),
                        p.get("date"), number(p.get("facilityId")));
                default -> editor.saveCountry(who, id, p.get("name"));
            };
            return PageResult.redirectTo("/" + type + "/" + saved);
        } catch (IllegalArgumentException e) {
            return showForm(400, type, id, p, e.getMessage());
        } catch (SQLException e) {
            return showForm(400, type, id, p,
                    "Could not save. A record with the same name may already exist.");
        }
    }

    private Map<String, String> facilityOptions() throws SQLException {
        Map<String, String> options = new LinkedHashMap<>();
        for (Facility f : database.getFacilities()) {
            options.put(String.valueOf(f.getId()), f.getName());
        }
        return options;
    }

    private Map<String, String> countryOptions() throws SQLException {
        Map<String, String> options = new LinkedHashMap<>();
        for (Country c : database.getCountries()) {
            options.put(String.valueOf(c.getId()), c.getName());
        }
        return options;
    }

    // ---------- Small helpers ----------

    private PageResult notFound() {
        return PageResult.error(404, "Not found", views.error("Record not found."));
    }

    // "agents" -> "Agent", "facilities" -> "Facility", "countries" -> "Country"
    private static String label(String type) {
        return switch (type) {
            case "agents" -> "Agent";
            case "facilities" -> "Facility";
            case "briefs" -> "Brief";
            case "missions" -> "Mission";
            default -> "Country";
        };
    }

    private static String value(Map<String, String> values, String name) {
        String value = values.get(name);
        return value == null ? "" : value;
    }

    // A positive whole number from a form field, or 0 if it is missing or not a number
    private static int number(String text) {
        try {
            return text == null ? 0 : Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
