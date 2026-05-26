# LangPic

Android app for young children to learn words. Displays a prompt word and a grid of images — the child selects the correct ones and presses Check. Supports mixed-language packs (English, Japanese, Polish, etc.) with TextToSpeech read-aloud. Lesson packs are imported from ZIP files.

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
  "items": [
    {
      "word1": "dog",
      "language1": "en",
      "word2": "pies",
      "language2": "pl",
      "images": [
        { "path": "images/dog.png", "correct": true },
        { "path": "images/cat.png", "correct": false }
      ]
    },
    {
      "word1": "What color is the sky?",
      "language1": "en",
      "word2": "Blue!",
      "images": []
    },
    {
      "word1": "Find all the fruits",
      "language1": "en",
      "word2": "Apple and banana!",
      "images": [
        { "path": "images/apple.png", "correct": true },
        { "path": "images/car.png",   "correct": false },
        { "path": "images/banana.png","correct": true }
      ]
    }
  ]
}
```

### Fields

| Field | Required | Description |
|---|---|---|
| `title` | No | Pack display name |
| `items[].word1` | Yes | Prompt word (TTS spoken in `language1`) |
| `items[].language1` | No | Locale for word1 (defaults to pack-level `language`) |
| `items[].word2` | No | Answer word shown after answering (TTS spoken in `language2`) |
| `items[].language2` | No | Locale for word2 (defaults to `language1`) |
| `items[].images[]` | No | Array of `{ "path", "correct" }` — paths relative to ZIP root |

### Game behavior per item

| Images | Behavior |
|---|---|
| 1 correct + distractors | Tap images to select, press **Check** to submit |
| Multiple correct | Tap to toggle each, press **Check** — all correct must be selected |
| None | Press **Check** to reveal word2 |

- Wrong or missed answers are re-queued until answered correctly.
- A helper character bounces at the bottom. After 5 seconds of inactivity it picks a correct image and gives a directional hint with a chat bubble.
- Score is shown as correct / total unique items.

Import the ZIP via the **Import Pack** button in the app. Duplicate titles are detected and warned about.

## Example pack

A sample manifest is at `data/manifest.json` with matching images in `data/images/`. Zip the `data/` folder and import it to test.

The full example spec is at `example.manifest.json`.

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
