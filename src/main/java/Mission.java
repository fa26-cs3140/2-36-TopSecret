// This is a mission record retrieved from the SQLite database.
// It holds the database ID, mission title, brief text, and date.
public class Mission {
    private final int id;
    private final String title;
    private final String brief;
    private final String date;

    // Constructor
    public Mission(int id, String title, String brief, String date) {
        this.id = id;
        this.title = title;
        this.brief = brief;
        this.date = date;
    }

    // Getter Methods
    public int getId() { return id; }
    public String getTitle() { return title; }
}
