package model;

// One search match (Member D): what kind of record it is, its id, and its title or name.
public class SearchResult {

    private final String type;
    private final int id;
    private final String title;

    public SearchResult(String type, int id, String title) {
        this.type = type;
        this.id = id;
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    // One readable line for the search screen, for example "Agent #2: Ghost"
    @Override
    public String toString() {
        return type + " #" + id + ": " + title;
    }
}
