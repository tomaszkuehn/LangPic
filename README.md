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
| Any | Tap images to toggle selection, press **Check** to submit — all correct (and no incorrect) must be selected |
| None | Press **Check** → correct answer shown → self-assess with emojis |

### Scoring

| Scenario | Points |
|---|---|
| Correct, no hint | 1.0 |
| Correct with hint, single correct image | 0.3 |
| Correct with hint, multiple correct images | 0.8 |
| No-image, self-assess 😊 | 0.8 (0.4 if after hint) |
| No-image, self-assess 😐 | 0.2 |
| No-image, self-assess 😢 | 0 — re-queued to very end |
| Wrong answer | 0 — re-queued |
| Repeated correct answer | 0 (already awarded) |

Max possible score = (items with images × 1.0) + (items without images × 0.8).

### Other features

- **Helper character** bounces at the bottom. After 8 seconds of inactivity it picks a correct image and shows a directional hint ("top-left one! 👆") with a chat bubble. Using the hint reduces points.
- **normal / shuffle** toggle at the bottom of the game screen. Shuffle mode randomly swaps `word1` ↔ `word2` per item (languages swap too). The swap decision is sticky — re-queued items keep the same mode.
- **High scores** per pack — shown in the game top bar and in Manage Packs. Saved automatically when a game finishes.
- **TTS check** on import — warns if any language in the pack is not supported by the device.

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
