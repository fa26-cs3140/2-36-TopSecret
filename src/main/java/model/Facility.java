package model;

public class Facility {
    private final int id;
    private String name;
    private String abbreviation;
    // HW7: the country this facility is assigned to. 0 means "no country".
    private final int countryId;

    // Facility with no country
    public Facility(int id, String name,
                    String abbreviation) {
        this(id, name, abbreviation, 0);
    }

    public Facility(int id, String name,
                    String abbreviation, int countryId) {
        if (countryId < 0) {
            throw new IllegalArgumentException(
                    "Country id cannot be negative");
        }
        this.id = id;
        this.name = name;
        this.abbreviation = abbreviation;
        this.countryId = countryId;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    // 0 when the facility has no country
    public int getCountryId() {
        return countryId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }
}