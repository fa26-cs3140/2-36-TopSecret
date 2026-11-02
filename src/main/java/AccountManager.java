import java.util.ArrayList;
import java.util.List;

// Rules for user accounts (Member D): validation, first account,
// adding users, logging in, and setting or changing passwords.
public class AccountManager {

    public static final String USERNAME_RULE =
            "Usernames must be at least 3 characters and use only letters, numbers, - and _.";
    public static final String PASSWORD_RULE =
            "Passwords must be at least 5 characters and include uppercase and lowercase letters.";

    private final UserStore store;

    public AccountManager(UserStore store) {
        this.store = store;
    }

    // At least 3 characters: letters, numbers, hyphens, underscores
    public static boolean isValidUsername(String username) {
        return username != null && username.matches("[A-Za-z0-9_-]{3,}");
    }

    // At least 5 characters with an uppercase and a lowercase letter.
    // Tabs and line breaks are not allowed because of the file format.
    public static boolean isValidPassword(String password) {
        return password != null
                && password.length() >= 5
                && !password.equals(password.toLowerCase())
                && !password.equals(password.toUpperCase())
                && !password.contains("\t")
                && !password.contains("\n")
                && !password.contains("\r");
    }

    public boolean hasAccounts() {
        return !store.loadAll().isEmpty();
    }

    public List<UserAccount> getUsers() {
        return store.loadAll();
    }

    // The account with this username, or null
    public UserAccount findUser(String username) {
        for (UserAccount u : store.loadAll()) {
            if (u.getUsername().equals(username)) {
                return u;
            }
        }
        return null;
    }

    // First account in an empty system. It is an admin.
    public UserAccount createFirstAccount(String username, String password) {
        if (hasAccounts()) {
            throw new IllegalStateException("Accounts already exist");
        }
        requireValid(username, password);
        UserAccount first = new UserAccount(username, password, true);
        store.saveAll(List.of(first));
        return first;
    }

    // A logged-in user adds a new user by username only.
    // The new user sets their own password at first login.
    public UserAccount addUser(UserAccount creator, String newUsername) {
        if (creator == null) {
            throw new IllegalStateException("You must be logged in to add a user");
        }
        if (!creator.isAdmin()) {
            throw new IllegalStateException("Only admins can add users");
        }
        if (!isValidUsername(newUsername)) {
            throw new IllegalArgumentException(USERNAME_RULE);
        }
        List<UserAccount> users = new ArrayList<>(store.loadAll());
        for (UserAccount u : users) {
            if (u.getUsername().equals(newUsername)) {
                throw new IllegalArgumentException("Username already exists");
            }
        }
        UserAccount added = new UserAccount(newUsername, null, false);
        users.add(added);
        store.saveAll(users);
        return added;
    }

    // The user if the username and password match, otherwise null
    public UserAccount login(String username, String password) {
        UserAccount user = findUser(username);
        if (user == null || !user.hasPassword() || !user.getPassword().equals(password)) {
            return null;
        }
        return user;
    }

    // True for a user who was added but has not set a password yet
    public boolean needsPassword(String username) {
        UserAccount user = findUser(username);
        return user != null && !user.hasPassword();
    }

    // First-login password for a new user
    public boolean setInitialPassword(String username, String password, String confirm) {
        if (!needsPassword(username) || !isValidPassword(password) || !password.equals(confirm)) {
            return false;
        }
        return savePassword(username, password);
    }

    // Change a password after checking the current one
    public boolean changePassword(String username, String oldPassword,
                                  String newPassword, String confirm) {
        if (login(username, oldPassword) == null
                || !isValidPassword(newPassword) || !newPassword.equals(confirm)) {
            return false;
        }
        return savePassword(username, newPassword);
    }

    // Access levels (Member A): an admin promotes a standard user to admin
    // or demotes an admin to standard. Admins cannot change their own level.
    public void setAdmin(UserAccount actor, String username, boolean makeAdmin) {
        if (actor == null || !actor.isAdmin()) {
            throw new IllegalStateException("Only admins can change access levels");
        }
        if (actor.getUsername().equals(username)) {
            throw new IllegalArgumentException("You cannot change your own access level");
        }
        List<UserAccount> users = new ArrayList<>(store.loadAll());
        for (UserAccount u : users) {
            if (u.getUsername().equals(username)) {
                u.setAdmin(makeAdmin);
                store.saveAll(users);
                return;
            }
        }
        throw new IllegalArgumentException("No user named " + username);
    }

    private boolean savePassword(String username, String password) {
        List<UserAccount> users = new ArrayList<>(store.loadAll());
        for (UserAccount u : users) {
            if (u.getUsername().equals(username)) {
                u.setPassword(password);
                store.saveAll(users);
                return true;
            }
        }
        return false;
    }

    private static void requireValid(String username, String password) {
        if (!isValidUsername(username)) {
            throw new IllegalArgumentException(USERNAME_RULE);
        }
        if (!isValidPassword(password)) {
            throw new IllegalArgumentException(PASSWORD_RULE);
        }
    }
}
