
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Mission record retrieved from the SQLite database.
public class Mission {
    private final int id;
    private final String title;
    private final String brief;
    private final String date;
    private final int facilityId;
    private final List<Integer> agentIds;

    // Original Homework 4 constructor
    public Mission(int id, String title,
                   String brief, String date) {
        this.id = id;
        this.title = title;
        this.brief = brief;
        this.date = date;
        this.facilityId = 0;
        this.agentIds = new ArrayList<>();
    }

    // Homework 5 constructor
    public Mission(int id, String title,
                   String brief, String date,
                   int facilityId,
                   List<Integer> agentIds) {

        if (facilityId <= 0) {
            throw new IllegalArgumentException(
                    "Mission must have a facility");
        }

        // stream check, because contains(null) crashes on lists made with List.of(...)
        if (agentIds == null || agentIds.isEmpty()
                || agentIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Mission must have at least one agent");
        }

        this.id = id;
        this.title = title;
        this.brief = brief;
        this.date = date;
        this.facilityId = facilityId;
        this.agentIds = new ArrayList<>(agentIds);
    }

    // Getter methods
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBrief() {
        return brief;
    }

    public String getDate() {
        return date;
    }

    public int getFacilityId() {
        return facilityId;
    }

    public List<Integer> getAgentIds() {
        return new ArrayList<>(agentIds);
    }

    // Equals method from teammate's code
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Mission)) {
            return false;
        }

        Mission other = (Mission) o;

        return id == other.id
                && Objects.equals(title, other.title)
                && Objects.equals(brief, other.brief)
                && Objects.equals(date, other.date);
    }

    // HashCode method from teammate's code
    @Override
    public int hashCode() {
        return Objects.hash(id, title, brief, date);
    }
}
