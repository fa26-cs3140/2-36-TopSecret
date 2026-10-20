
public class Agent {
    private final int id;
    private String name;
    private String dateOfBirth;
    private String dateOfDeath;
    private String notes;
    private int facilityId;

    public Agent(int id, String name, String dateOfBirth,
                 String dateOfDeath, String notes,
                 int facilityId) {
        this.id = id;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.dateOfDeath = dateOfDeath;
        this.notes = notes;
        this.facilityId = facilityId;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getDateOfDeath() {
        return dateOfDeath;
    }

    public String getNotes() {
        return notes;
    }

    public int getFacilityId() {
        return facilityId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setDateOfDeath(String dateOfDeath) {
        this.dateOfDeath = dateOfDeath;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setFacilityId(int facilityId) {
        this.facilityId = facilityId;
    }
}

