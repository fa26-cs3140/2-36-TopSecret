package model;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

// Web sessions saved in the SQLite database (Member D), so the terminal
// can list and delete sessions while the web server runs separately.
// Stub: methods are implemented in a later commit.
public class SqliteSessionStore implements SessionStore {

    public SqliteSessionStore(Path dbFile) {
        this(dbFile, Clock.systemDefaultZone());
    }

    // The clock can be fixed in tests to check the 30 minute timeout
    public SqliteSessionStore(Path dbFile, Clock clock) {
    }

    @Override
    public String create(String username) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String touch(String token) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<WebSession> list() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean delete(String token) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
