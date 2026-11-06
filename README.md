# 2-36-TopSecret

CS 3140 Homework 6: agents log in and work with mission briefs, agents, facilities, and missions stored in a SQLite database, from the terminal or (read only) from a web browser. It has multiple user accounts, admin/standard access levels, an audit log, and messages for agents. The code is organized into model, view, and controller packages.

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

**Web view (read only):**

```
java -jar build/libs/TopSecret.jar -web
```

Then open http://localhost:3002/ and log in. Create an account in the terminal first; the web view will not start without one. Close the browser (or click Logout) to end the session.

- **First run (no `users.cip`):** create the first account. It is an admin. Usernames need at least 3 characters (letters, numbers, `-`, `_`); passwords need at least 5 characters with uppercase and lowercase letters. The program then exits; run it again to log in.
- **Top menu:** `1` existing mission menu (list, read, search missions), `2` records menu, `3` messages for agents, `4` missions and agent assignments (admins), `0` log out.
- **Records menu:** choose Mission Briefs, Agents, or Facilities, then list, read, add, update, or delete entries (add, update, and delete are admin only).
- **Admin options:** `4` view the audit log, `5` add a user (username only; they set their own password at first login), `6` promote or demote another user.
- **Change password:** `./gradlew run --args="--change-password"`

## Team

- **Member A (Model: missions and joint operations):** Lucas Ung
- **Member B (Control: web server, login, session cookie):** Lina Toufik
- **Member C (View: HTML view and wireframes):** Britney Nguyen
- **Member D (Messages for agents):** Smayan Sangoju

## Folders

- `database/`: sample data (`mission_briefs.tsv`, `agents.tsv`, `facilities.tsv`); `missions.db` is created here on first run
- `data/`: Homework 3 ciphered mission files
- `ciphers/`: key files (`key.txt` is the default key)
- `docs/`: documentation for each part of the program
- `src/main/java/`: `model`, `view`, and `controller` packages, plus `TopSecret.java` (entry point)
- `src/test/java/`: JUnit tests, in the same packages as the classes they test

## Notes

- Bad menu input, a wrong password, or a database error prints a message instead of crashing.
- See `docs/` for each part's design and tests, `docs/changes.txt` for change notes, `docs/schema.txt` for every database table, `docs/UMLClassDiagram.pdf` and `docs/UML_ERD.pdf` for the diagrams, and `docs/Wireframes.pdf` for the web view.
- Run the tests with `./gradlew test`.