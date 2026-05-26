# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

An Android app for young children that displays a word and two images — the child taps the matching image. Supports English and Japanese words with TextToSpeech read-aloud. The project is defined by `PROMPT.txt` and **has not been scaffolded yet**.

## Tech stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture:** MVVM with Repository pattern
- **Persistence:** Room database
- **State management:** ViewModel + StateFlow
- **Speech:** Android TextToSpeech (Locale.ENGLISH / Locale.JAPANESE or `Locale("ja","JP")`)
- **ZIP handling:** `ZipInputStream` for importing lesson packs

## Domain model

Lesson packs are ZIP files containing a `manifest.json` and `images/` directory. Each manifest item pairs a word with a correct image and a wrong image, tagged with a language code (`"en"` or `"ja"`). Imported packs are unpacked into app-private storage and stored in the local database.

## Project structure (when scaffolding)

The prompt asks for these layers, each in its own package:
- **data** — Room entities, DAOs, database class, repository implementations
- **domain** — models separate from DB entities
- **ui** — Composable screens (Home, Game, Import, PackManagement)
- **viewmodel** — ViewModels per screen
- **service** — TTS helper (init once, release correctly, pre-check language support), ZIP importer + manifest parser

Screens: Home → Play / Import ZIP / Manage Packs → Game (word + 2 images + speaker button + feedback) / Import (file picker + validation + summary) / Pack Management (list, enable/disable, delete).

## Key behavioral rules

- Two image cards per round, displayed in randomized order.
- Correct tap → happy face → next item after short delay.
- Wrong tap → sad face + reveal correct image → move on.
- Auto-speak the word on display, plus a speaker replay button.
- Fall back to device default TTS locale if the requested language is unsupported.
- All interactions must be large-tap-friendly, high-contrast, and content-described for accessibility.
- Operates fully offline.
