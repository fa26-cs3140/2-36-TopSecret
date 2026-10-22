import java.util.List;

// Where user accounts are saved (Member D).
// AccountManager only talks to this interface, so tests can use a mock.
public interface UserStore {

    // All saved accounts, or an empty list if there are none yet
    List<UserAccount> loadAll();

    // Replaces the saved accounts with this list
    void saveAll(List<UserAccount> users);
}
