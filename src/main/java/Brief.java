public class Brief {
    private final int id;
    private String title;
    private String date;
    private String text;

    public Brief(int id, String title,
                 String date, String text) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.text = text;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDate() {
        return date;
    }

    public String getText() {
        return text;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setText(String text) {
        this.text = text;
    }
}
