# Lethe

*Lethe is the river of forgetfulness in the Greek underworld. Words dissolve on screen as if in
its waters, yet remain whole in the note.*

A minimalist note-taking app for Android. In fade mode, only the word you are typing is visible:
after a space, it fades away. The full text appears once you leave the mode.

## Features

- Fade mode. The screen is split between a white canvas and the keyboard; finished words fade away and can no longer be edited.
- Note editor with the full text, plus Share and Delete actions.
- Continue writing in fade mode from any open note.
- Note list with creation date and time.
- "New entry" shortcut on a long press of the app icon.
- Portrait orientation only.

## Privacy

- Faded words cannot be deleted or changed, and the cursor cannot be moved back.
- The keyboard receives `IME_FLAG_NO_PERSONALIZED_LEARNING`, so input is not used to train it.
- Screen content is hidden in the Recents view.
- The app does not use the network and requests no permissions.

## Storage

Each note is stored as a UTF-8 text file in the app's internal storage:
`files/notes/<timestamp>.txt`, where `timestamp` is the creation time in milliseconds.
Changes are saved automatically.

## Building

Requirements: JDK 17+, Android SDK 37.

```bash
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

In Android Studio: Build → Generate App Bundles or APKs → Generate APKs.

## Technical details

| Parameter | Value |
|---|---|
| Language | Java 17, no third-party libraries |
| minSdk / targetSdk / compileSdk | 33 / 36 / 37 |
| Build | Android Gradle Plugin 9.5, Gradle 9.6 |

## Structure

| Class | Purpose |
|---|---|
| `MainActivity` | Note list |
| `WriteActivity` | Fade mode |
| `NoteActivity` | Viewing and editing a note |
| `NoteStore` | Reading and writing note files |
| `TrapEditText` | Input field whose cursor always stays at the end of the text |
| `Ui` | Date formatting and word counts |

## Third-party assets

The Carlito font is distributed under the SIL Open Font License 1.1; see `FONT-LICENSE-Carlito.txt`.
