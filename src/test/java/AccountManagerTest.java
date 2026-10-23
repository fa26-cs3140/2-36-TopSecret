import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Unit tests: the user file is replaced by a Mockito mock of UserStore
class AccountManagerTest {

    private final UserStore store = mock(UserStore.class);
    private final AccountManager accounts = new AccountManager(store);

    // Makes the mock store return these accounts
    private void storeHas(UserAccount... users) {
        when(store.loadAll()).thenReturn(new ArrayList<>(List.of(users)));
    }

    @SuppressWarnings("unchecked")
    private List<UserAccount> savedUsers() {
        ArgumentCaptor<List<UserAccount>> captor = ArgumentCaptor.forClass(List.class);
        verify(store).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void validUsernamesFollowTheRules() {
        assertTrue(AccountManager.isValidUsername("abc"));
        assertTrue(AccountManager.isValidUsername("Agent_007-x"));
        assertFalse(AccountManager.isValidUsername("ab"));
        assertFalse(AccountManager.isValidUsername("agent smith"));
        assertFalse(AccountManager.isValidUsername("agent!"));
        assertFalse(AccountManager.isValidUsername(null));
    }

    @Test
    void validPasswordsNeedFiveCharactersAndBothCases() {
        assertTrue(AccountManager.isValidPassword("Abcde"));
        assertFalse(AccountManager.isValidPassword("Abcd"));
        assertFalse(AccountManager.isValidPassword("abcde"));
        assertFalse(AccountManager.isValidPassword("ABCDE"));
        assertFalse(AccountManager.isValidPassword("12345"));
        assertFalse(AccountManager.isValidPassword("Abc\tde"));
        assertFalse(AccountManager.isValidPassword("Abc\nde"));
        assertFalse(AccountManager.isValidPassword("Abc\rde"));
        assertFalse(AccountManager.isValidPassword(null));
    }

    @Test
    void hasAccountsChecksTheStore() {
        storeHas();
        assertFalse(accounts.hasAccounts());
        storeHas(new UserAccount("agent", "Secret1", true));
        assertTrue(accounts.hasAccounts());
    }

    @Test
    void getUsersReturnsAllAccounts() {
        storeHas(new UserAccount("agent", "Secret1", true), new UserAccount("bob", null, false));
        assertEquals(2, accounts.getUsers().size());
    }

    @Test
    void findUserReturnsMatchOrNull() {
        storeHas(new UserAccount("agent", "Secret1", true));
        assertEquals("agent", accounts.findUser("agent").getUsername());
        assertNull(accounts.findUser("nobody"));
    }

    @Test
    void firstAccountIsSavedAsAdmin() {
        storeHas();
        UserAccount first = accounts.createFirstAccount("agent", "Secret1");
        assertTrue(first.isAdmin());
        List<UserAccount> saved = savedUsers();
        assertEquals(1, saved.size());
        assertEquals("agent", saved.get(0).getUsername());
        assertEquals("Secret1", saved.get(0).getPassword());
        assertTrue(saved.get(0).isAdmin());
    }

    @Test
    void firstAccountCannotBeCreatedTwice() {
        storeHas(new UserAccount("agent", "Secret1", true));
        assertThrows(IllegalStateException.class, () -> accounts.createFirstAccount("other", "Secret1"));
        verify(store, never()).saveAll(anyList());
    }

    @Test
    void firstAccountMustFollowRules() {
        storeHas();
        assertThrows(IllegalArgumentException.class, () -> accounts.createFirstAccount("ab", "Secret1"));
        assertThrows(IllegalArgumentException.class, () -> accounts.createFirstAccount("agent", "secret1"));
        verify(store, never()).saveAll(anyList());
    }

    @Test
    void loggedInUserCanAddUserWithoutPassword() {
        UserAccount admin = new UserAccount("agent", "Secret1", true);
        storeHas(admin);
        UserAccount added = accounts.addUser(admin, "bob_2");
        assertEquals("bob_2", added.getUsername());
        assertFalse(added.hasPassword());
        assertFalse(added.isAdmin());
        assertEquals(2, savedUsers().size());
    }

    @Test
    void addUserRequiresLoggedInUser() {
        assertThrows(IllegalStateException.class, () -> accounts.addUser(null, "bob"));
        verify(store, never()).saveAll(anyList());
    }

    @Test
    void addUserRejectsInvalidOrDuplicateUsername() {
        UserAccount admin = new UserAccount("agent", "Secret1", true);
        storeHas(admin);
        assertThrows(IllegalArgumentException.class, () -> accounts.addUser(admin, "b"));
        assertThrows(IllegalArgumentException.class, () -> accounts.addUser(admin, "agent"));
        verify(store, never()).saveAll(anyList());
    }

    @Test
    void loginReturnsUserOnlyForCorrectPassword() {
        storeHas(new UserAccount("agent", "Secret1", true), new UserAccount("bob", null, false));
        assertEquals("agent", accounts.login("agent", "Secret1").getUsername());
        assertNull(accounts.login("agent", "wrong"));
        assertNull(accounts.login("bob", ""));
        assertNull(accounts.login("nobody", "Secret1"));
    }

    @Test
    void needsPasswordOnlyForUsersWithoutOne() {
        storeHas(new UserAccount("agent", "Secret1", true), new UserAccount("bob", null, false));
        assertTrue(accounts.needsPassword("bob"));
        assertFalse(accounts.needsPassword("agent"));
        assertFalse(accounts.needsPassword("nobody"));
    }

    @Test
    void newUserSetsInitialPassword() {
        storeHas(new UserAccount("bob", null, false));
        assertTrue(accounts.setInitialPassword("bob", "BobPass", "BobPass"));
        assertEquals("BobPass", savedUsers().get(0).getPassword());
    }

    @Test
    void initialPasswordMustMatchFollowRulesAndOnlyBeSetOnce() {
        storeHas(new UserAccount("agent", "Secret1", true), new UserAccount("bob", null, false));
        assertFalse(accounts.setInitialPassword("bob", "BobPass", "Other1"));
        assertFalse(accounts.setInitialPassword("bob", "bobpass", "bobpass"));
        assertFalse(accounts.setInitialPassword("agent", "NewPass", "NewPass"));
        verify(store, never()).saveAll(anyList());
    }

    @Test
    void changePasswordAfterCheckingCurrentPassword() {
        storeHas(new UserAccount("agent", "Secret1", true));
        assertTrue(accounts.changePassword("agent", "Secret1", "Better2", "Better2"));
        assertEquals("Better2", savedUsers().get(0).getPassword());
    }

    @Test
    void changePasswordFailsForWrongOldPasswordOrBadNewPassword() {
        storeHas(new UserAccount("agent", "Secret1", true));
        assertFalse(accounts.changePassword("agent", "wrong", "Better2", "Better2"));
        assertFalse(accounts.changePassword("agent", "Secret1", "better2", "better2"));
        assertFalse(accounts.changePassword("agent", "Secret1", "Better2", "Better3"));
        verify(store, never()).saveAll(anyList());
    }

    @Test
    void onlyAdminsCanAddUsers() {
        storeHas(new UserAccount("bob", "BobPass", false));
        assertThrows(IllegalStateException.class,
                () -> accounts.addUser(new UserAccount("bob", "BobPass", false), "carol"));
        verify(store, never()).saveAll(anyList());
    }

    @Test
    void adminPromotesAndDemotesOtherUsers() {
        UserAccount admin = new UserAccount("agent", "Secret1", true);
        storeHas(admin, new UserAccount("bob", "BobPass", false));
        accounts.setAdmin(admin, "bob", true);
        assertTrue(savedUsers().get(1).isAdmin());
    }

    @Test
    void accessLevelRules() {
        UserAccount admin = new UserAccount("agent", "Secret1", true);
        UserAccount bob = new UserAccount("bob", "BobPass", false);
        storeHas(admin, bob);
        assertThrows(IllegalStateException.class, () -> accounts.setAdmin(bob, "agent", false));
        assertThrows(IllegalStateException.class, () -> accounts.setAdmin(null, "bob", true));
        assertThrows(IllegalArgumentException.class, () -> accounts.setAdmin(admin, "agent", false));
        assertThrows(IllegalArgumentException.class, () -> accounts.setAdmin(admin, "nobody", true));
        verify(store, never()).saveAll(anyList());
    }
}
