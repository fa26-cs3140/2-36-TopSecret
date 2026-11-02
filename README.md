# 2-36-TopSecret

CS 3140 Homework 5: an interactive command-line tool where agents log in and view, add, update, and delete mission briefs, agents, and facilities stored in a SQLite database, with multiple user accounts, admin/standard access levels, and an audit log.

## How to Run

```
./gradlew run
```

Or build the jar (it includes all dependencies) and run it from the project root:

```
./gradlew build
java -jar build/libs/TopSecret.jar
```

Run the tests with `./gradlew test`.

- **First run (no `users.cip`):** create the first account. It is an admin. Usernames need at least 3 characters (letters, numbers, `-`, `_`); passwords need at least 5 characters with uppercase and lowercase letters. The program then exits; run it again to log in.
- **Top menu:** `1` existing mission menu (list, read, search missions), `2` Homework 5 records menu, `0` log out.
- **Records menu:** choose Mission Briefs, Agents, or Facilities, then list, read, add, update, or delete entries (add, update, and delete are admin only).
- **Admin options:** `4` view the audit log, `5` add a user (username only; they set their own password at first login), `6` promote or demote another user.
- **Change password:** `./gradlew run --args="--change-password"`

## Team

- **Member A (Access levels):** Lucas Ung
- **Member B (User interface):** Lina Toufik
- **Member C (Model / database):** Britney Nguyen
- **Member D (User accounts and audit log):** Smayan Sangoju

## Folders

- `database/`: sample data (`mission_briefs.tsv`, `agents.tsv`, `facilities.tsv`); `missions.db` is created here on first run
- `data/`: Homework 3 ciphered mission files
- `ciphers/`: key files (`key.txt` is the default key)
- `docs/`: documentation for each part of the program
- `src/`: source code and JUnit tests

## Notes

- Bad menu input, a wrong password, or a database error prints a message instead of crashing.
- See `docs/` for each part's design and tests, `docs/changes.txt` for change notes, and `docs/schema.txt` for the database table.
- Run the tests with `./gradlew test`.