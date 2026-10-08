import java.util.Objects;

/**
 * One mission record: title, brief, and date
 */
public class Mission {

    private final int id;
    private final String title;
    private final String brief;
    private final String date;

    public Mission(int id, String title, String brief, String date) {
        this.id = id;
        this.title = title;
        this.brief = brief;
        this.date = date;
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(id, title, brief, date);
    }

    @Override
    public String toString() {
        return "Mission{id=" + id + ", title='" + title + "', date='" + date + "'}";
    }
}