# 2-36-TopSecret

CS 3140 Homework 3: a command-line tool that lets agents list and read secret mission files. Files are ciphered and are deciphered using a key before being displayed.

## What It Does

- **No arguments:** lists the available mission files, numbered (`01 carnivore.cip`, `02 cointelpro.cip`).
- **A file number:** deciphers that file with the default key and prints it (`./gradlew run --args="01"`).
- **A file number and a key file:** deciphers the file with an alternate key instead (`./gradlew run --args="01 key.txt"`).

The program exits after showing the list, the file, or an error message.

## Team

- **Member A (User Interface):** Lucas Ung
- **Member B (Program Control):** Lina Toufik
- **Member C (File Handler):** Britney Nguyen
- **Member D (Cipher):** Smayan Sangoju

## Folders

- `data/`: ciphered mission files
- `ciphers/`: key files (`key.txt` is the default key)
- `docs/`: documentation for each part of the program
- `src/`: source code and JUnit tests

## Notes

- The second argument is the name of a key file in the `ciphers/` folder.
- If a file or key can't be found or is invalid, the program prints an error and exits instead of crashing.
- Run the tests with `./gradlew test`.