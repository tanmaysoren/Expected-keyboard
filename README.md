# Expected Keyboard

A lightweight, privacy-focused virtual keyboard for Android with intuitive swipe gestures, expanded key combinations, and fully customizable layouts.

No ads, no tracking, 100% offline — all predictions and dictionary learning stay on your device.

---

## Features

### Swipe & Gesture Typing
- **9-direction swipe** per key — center, north/south/east/west and diagonals produce different characters without switching layouts
- **Space-bar slider** — swipe left/right to move by word (`Ctrl+Arrow` in Termux, word boundaries in normal editors)
- **Cursor sliders** — dedicated horizontal/vertical sliders for precise movement
- **Circle gesture** — adjust sensitivity or disable in settings
- **Long-press** with configurable timeout/interval and key repeat

### Prediction & Autocorrection
- Offline `PredictionEngine` — Trie prefix search, bigram next-word predictions, spatial keyboard-distance autocorrection
- Common typo map (`teh→the`, `dont→don't`) + user-learned dictionary (bigrams, frequency, capped at 2000 words)
- 5 word suggestions + 1 emoji suggestion in immersive suggestion bar with adjustable font size (0.7×–1.8×)
- Next-word suggestions when bar is empty

### Layouts & Languages
- 100+ layouts: `latn_qwerty_us` (default), `colemak`, `dvorak`, `azerty`, `qwertz`, Cyrillic, Indic, Arabic, Hebrew, etc.
- Add layouts from **Settings → Layout** or instantly via the **utility bar → Layout** switcher pane (single tap)
- Custom layouts via in-app XML editor (`CustomLayoutEditDialog`)
- Number row (with/without symbols) + NumPad (low/high first)

### Utility Bar (above suggestions)
Toggle via chevron: Clipboard, Text Edit & navigation, Undo/Redo, Numpad, Emoji, Layout switcher, Theme switcher, Floating mode, Settings — with simple flat styling that follows the keyboard theme.

### Panes
- **Editing pane** — line start/end, page up/down, arrow keys, word jump, select mode, backspace/delete, select-all, enter — all using normal key styling
- **Clipboard pane** — history (configurable duration) + pinned clips
- **Emoji pane** — categorized grid
- **Theme switcher** — instant preview of Light/Dark/Black/Desert/Jungle/Monet/Frosted Obsidian etc.
- **Floating mode** — drag handle to move, corner handle to resize, persisted per orientation/fold state

### Style & Themes
- 15+ built-in themes (`themes.xml`): Light, Dark, Black, White, Desert, Jungle, MonetLight/Dark, RosePine, Dracula, Frosted Obsidian, etc.
- Per-theme `colorKeyboard`, `colorKey`, `colorLabel`, borders, radius — all theme-driven
- Adjustable character size, key spacing, margins, keyboard height (portrait/landscape/folded), opacity, border radius/width, label brightness

### Privacy
- No network permission required for typing; predictions are fully offline via `BuiltinLexicon`, `TrieDictionary`, `UserDictionary`, `SpatialDistance`
- No data leaves the device

## Installation

1. Install APK: `app/build/outputs/apk/release/app-release.apk` (optimized ~2.5 MB with R8)
2. Android **Settings → System → Languages & input → On-screen keyboard → Manage keyboards → Enable Expected Keyboard**
3. Switch input method to *Expected Keyboard* (via expandable notification or long-press on space)
4. Grant no extra permissions; optionally download dictionaries via **Settings → Dictionaries** or in-app prompt

## Usage

- **Tap** for center character, **swipe** toward corner/edge for alternates
- **Hold Shift** → caps, **double-tap Shift** → caps lock (configurable)
- **Swipe space** → word navigation; **sliders** → cursor/word deletion
- **Suggestion bar** — tap word to insert, bold = autocorrect candidate; use slider in **Settings → Prediction → Suggestion text size**
- **Utility bar** — tap chevron, then icons; typing auto-returns to suggestions unless you manually locked the bar

## Project Structure

```
app/src/main/java/expected/keyboard2/
  Keyboard2.java              — InputMethodService entry
  Keyboard2View.java          — canvas renderer
  KeyEventHandler.java        — dispatcher (InputConnection)
  Pointers.java               — multi-touch gesture engine
  Config.java                 — SharedPreferences wrapper (characterSize, suggestionFontScale, …)
  suggestions/CandidatesView.java — suggestion + utility bar UI
  prediction/                 — PredictionEngine, Trie, UserDictionary, BuiltinLexicon
  layout/LayoutSwitcherPaneView.java — in-keyboard layout picker
  theme/ThemeSwitcherPaneView.java   — theme picker
  EditingPaneView.java, ClipboardHistoryView.java, …

app/src/main/res/xml/         — layout definitions (latn_*.xml, bottom_row.xml, numeric.xml)
app/src/main/res/values/themes.xml — all themes
```

**Layout XML syntax:** each `<key>` defines `c` (center) + `n/ne/e/se/s/sw/w/nw` swipe outputs; attributes `width`, `shift`, `role="action|space_bar"`.

## Customization

| Task | File |
|---|---|
| Add flick chars | `res/xml/latn_qwerty_us.xml` |
| Bottom row | `res/xml/bottom_row.xml` |
| Theme colors | `res/values/themes.xml` + `Theme.java` |
| Suggestion UI | `suggestions/CandidatesView.java` |
| Dictionary | `prediction/BuiltinLexicon.java` |

## Build From Source

**Prereqs:** JDK 17, Android SDK 36 + build-tools 36.1.0

```bash
# Install SDK (example Windows)
sdkmanager "platforms;android-36" "build-tools;36.1.0" "platform-tools"

# Debug (fast, ~23 MB)
./gradlew assembleDebug

# Release optimized (R8 + shrinkResources, ~2.5 MB, 2.6 MB with current configs)
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
# Signed with debug.keystore fallback if upload keystore absent
```

**Gradle:** 9.3.1, AGP 9.1.1, `lint.abortOnError=false`

## Privacy & License

This project is open-source and offline-first. No API keys required . See source for license.

## App Info

- **Namespace:** `expected.keyboard2`
- **Application ID:** `com.expected.keyboard` (`app/build.gradle.kts:13`)
- **Min SDK:** 24, **Target/Compile SDK:** 36
