# LangPic

Android app for young children to learn words in English and Japanese. Shows a word with two images — the child taps the matching one. TextToSpeech reads each word aloud. Lesson packs are imported from ZIP files.

## Prerequisites

- **Android Studio** (Hedgehog 2023.1+ or later)
- **Android SDK 34** (installed via Android Studio SDK Manager)
- **JDK 17**

## Build

Open the project in Android Studio and sync Gradle, then:

```bash
# Debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Run lint checks
./gradlew lint
```

The APK is output to `app/build/outputs/apk/debug/`.

## Lesson pack format

Create a ZIP file with this structure:

```
mypack.zip
├── manifest.json
├── images/
│   ├── dog.png
│   ├── cat.png
│   └── ...
```

`manifest.json`:

```json
{
  "title": "My Pack",
  "language": "en",
  "items": [
    {
      "word": "dog",
      "language": "en",
      "correctImage": "images/dog.png",
      "wrongImage": "images/cat.png"
    }
  ]
}
```

- `language` at the pack level is optional and acts as a default for items that don't specify their own.
- Each item can override `language` (e.g. `"ja"` for Japanese), so a single pack can mix languages.
- Image paths are relative to the ZIP root.

Import the ZIP via the **Import Pack** button in the app.

## Project structure

```
app/src/main/java/com/example/langpic/
├── data/               Room entities, DAOs, database, repository
├── domain/model/       Domain models (separate from DB entities)
├── service/            ZipImporter, ManifestParser, TtsHelper
└── ui/
    ├── theme/          Material3 color/type/theme
    ├── navigation/     NavGraph + Screen routes
    └── screen/
        ├── home/       Main menu (Learn, Import, Manage)
        ├── game/       Word + image matching game
        ├── importzip/  ZIP file picker + import flow
        └── manage/     Pack list with enable/delete
```

## Tech stack

- Kotlin + Jetpack Compose + Material3
- Room (local DB), Navigation Compose, Coil (image loading)
- MVVM with StateFlow
