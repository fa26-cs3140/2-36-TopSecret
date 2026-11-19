package model;

import java.util.Objects;

// A country that facilities are assigned to (HW7, Member A).
// A country may have zero or more facilities. A Country object is a snapshot
// of a database row; changes go through ModelDatabase and are reloaded.
public class Country {
    private final int id;
    private final String name;

    public Country(int id, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Country name is required");
        }
        this.id = id;
        this.name = name.trim();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Country)) {
            return false;
        }
        Country other = (Country) o;
        return id == other.id && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return name;
    }
}