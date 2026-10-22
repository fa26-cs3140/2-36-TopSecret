// One user of the system (Member D).
// The password is null until a new user sets it on their first login.
// The admin flag is stored here; Member A's access-level code decides what it allows.
public class UserAccount {

    private final String username;
    private String password;
    private boolean admin;

    public UserAccount(String username, String password, boolean admin) {
        this.username = username;
        this.password = password;
        this.admin = admin;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    // True once the user has created a password
    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }
}
