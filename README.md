# 2-36-TopSecret

CS 3140 Homework 7: agents log in and work with mission briefs, agents, facilities, countries, and missions stored in a SQLite database, from the terminal or from a web browser. Admins can add and edit records in the web view. It has multiple user accounts, admin/standard access levels, an audit log, messages for agents, and web sessions that time out. The code is organized into model, view, and controller packages.

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

**Web view:**

```
java -jar build/libs/TopSecret.jar -web
```

Then open http://localhost:3002/ and log in. Create an account in the terminal first; the web view will not start without one.

- Every page has the same menu (Home, Agents, Facilities, Countries, Missions, Briefs, Messages, Log out). Admins also see Audit Log.
- Each list links to a record's page. A record's page shows its related records as links (a facility shows its country, agents, and missions; an agent shows its missions; a mission shows its briefs).
- Admins can add a record ("Add new" under each list) and edit one ("Edit" on its page). Standard users can only view.
- Messages: send a message to an agent, and read the messages addressed to your own username.
- A session ends after 30 minutes without a request, when you log out, or when an admin deletes it from the terminal. New users and passwords are managed in the terminal only.

**Help:** `java -jar build/libs/TopSecret.jar -h` lists the command line options.

- **First run (no `users.cip`):** create the first account. It is an admin. Usernames need at least 3 characters (letters, numbers, `-`, `_`); passwords need at least 5 characters with uppercase and lowercase letters. The program then exits; run it again to log in.
- **Top menu:** `1` existing mission menu (list, read, search missions), `2` records menu, `3` messages for agents, `4` search all records (every text field of missions, briefs, agents, facilities, and countries), `5` missions and agent assignments (admins), `6` web sessions (admins), `0` log out.
- **Web sessions (admins):** list every active web session (user, token, last activity) and delete one, which makes that browser log in again.
- **Records menu:** choose Mission Briefs, Agents, or Facilities, then list, read, add, update, or delete entries (add, update, and delete are admin only).
- **Admin options:** `4` view the audit log, `5` add a user (username only; they set their own password at first login), `6` promote or demote another user.
- **Change password:** `./gradlew run --args="--change-password"`

## Team

- **Member A (View: styling, menu, countries, help option):** Lucas Ung
- **Member B (Control: add and edit records in the web view):** Lina Toufik
- **Member C (Web messaging center):** Britney Nguyen
- **Member D (Model: create/update functions, audit, related records, search, session access control):** Smayan Sangoju

## Folders

- `database/`: sample data (`mission_briefs.tsv`, `agents.tsv`, `facilities.tsv`, `countries.tsv`); `missions.db` is created here on first run
- `ciphers/`: cipher key (`key.txt`) used to encipher `users.cip`
- `docs/`: documentation for each part of the program
- `src/main/java/`: `model`, `view`, and `controller` packages, plus `TopSecret.java` (entry point)
- `src/test/java/`: JUnit tests, in the same packages as the classes they test

## Notes

- Bad menu input, a wrong password, or a database error prints a message instead of crashing.
- See `docs/` for each part's design and tests, `docs/changes.txt` for change notes, `docs/schema.txt` for every database table, `docs/UMLClassDiagram.pdf` and `docs/UML_ERD.pdf` for the diagrams, and `docs/Wireframes.pdf` for the web view.
- Run the tests with `./gradlew test`.