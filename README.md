# 2-36-TopSecret

CS 3140 Homework 4: an interactive command-line tool that lets agents log in and list, read, and search secret mission briefs stored in a SQLite database.

## How to Run

```
./gradlew run
```

Or build the jar and run it from the project root:

```
./gradlew build
java -jar build/libs/TopSecret.jar
```

- **First run (no `credentials.cip`):** you are asked to create a username (lowercase letters only) and a password (at least 5 characters). The program saves them ciphered in `credentials.cip` and exits. Run it again to log in.
- **Log in:** enter your username and password. The repo includes a test account: username `agent`, password `secret123`.
- **Menu:** `1` list missions, `2` read a mission by number, `3` search briefs for a word or phrase, `4` exit.
- **Change password:** `./gradlew run --args="--change-password"`

## Team

- **Member A (Database):** Lucas Ung
- **Member B (Password / Login):** Lina Toufik
- **Member C (Search):** Britney Nguyen
- **Member D (User Interface):** Smayan Sangoju

## Folders

- `database/`: `mission_briefs.tsv` import file (the database file is created here on first run)
- `data/`: Homework 3 ciphered mission files
- `ciphers/`: key files (`key.txt` is the default key)
- `docs/`: documentation for each part of the program
- `src/`: source code and JUnit tests

## Notes

- Bad menu input, a wrong password, or a database error prints a message instead of crashing.
- See `docs/` for each part's design and tests, `docs/changes.txt` for change notes, and `docs/schema.txt` for the database table.
- Run the tests with `./gradlew test`.