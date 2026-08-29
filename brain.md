# Unexpected Keyboard Architecture & Developer Guide (brain.md)

This file contains comprehensive reference information and component mappings for the keyboard codebase. Use this as a central guide to rapidly locate files, understand system architecture, and make targeted modifications without inspecting every source file.

---

## 1. Project Overview & Tech Stack
- **Project Type:** Android Input Method Engine (IME)
- **Package Name:** `expected.keyboard2`
- **Application Class / Service:** `Keyboard2` (extends `android.inputmethodservice.InputMethodService`)
- **Key Characteristics:** 
  - High performance, lightweight, privacy-focused.
  - Multi-direction flick/swipe system (9 directions per key: center, cardinal, and diagonal).
  - Built-in sliders for precise cursor movement, selection, and deletion.
  - Terminal-friendly (`TYPE_NULL` / Termux) fallbacks with explicit Ctrl+Arrow key event synthesis.
  - Offline local dictionary and suggestion bar.

---

## 2. Source Code Map & Component Responsibilities

### Core IME & Event Pipeline (`/app/src/main/java/expected/keyboard2/`)

| File | Primary Role & Description | Key Methods / Variables |
|---|---|---|
| **`Keyboard2.java`** | Main `InputMethodService` entry point. Handles IME lifecycle, window creation, configuration changes, layout switching, and coordinating the keyboard view with suggestions. | `onCreateInputView()`, `onStartInputView()`, `onWindowShown()`, `loadLayout()`, `switch_layout()` |
| **`Keyboard2View.java`** | Custom Android `View` that renders the keyboard canvas. Draws keys, icons, labels, accent indicators, and touch feedback. Dispatches touch events to `Pointers`. | `onDraw()`, `onTouchEvent()`, `drawKey()`, `drawKeyLabel()`, `setTheme()` |
| **`KeyEventHandler.java`** | Core event dispatcher for text input, cursor manipulation, key generation, dead keys, and macros. Connects IME with `InputConnection`. | `handle_key()`, `handle_slider()`, `move_cursor()`, `send_ctrl_dpad()`, `send_key_down_up()`, `handle_space_bar()`, `handle_backspace()` |
| **`Pointers.java`** | Multi-touch gesture engine. Tracks finger pointer coordinates, detects flick angles, triggers sliders, repeat timers, and calculates swipe distance thresholds. | `onTouchEvent()`, `handleDown()`, `handleMove()`, `handleUp()`, `startSliding()`, `detectDirection()` |
| **`KeyValue.java`** | Key representation and encoding. Encapsulates unicode characters, actions, editing keys, sliders, dead keys, and modifiers. | `charKey()`, `actionKey()`, `editingKey()`, `sliderKey()`, `Slider` enum, `Editing` enum |
| **`KeyModifier.java`** | Modifier manager (Shift, Ctrl, Alt, Meta/Fn, CapsLock, Compose). Converts keys according to active modifier states. | `apply_modifiers()`, `apply_shift()`, `apply_ctrl()`, `is_shift_active()` |
| **`KeyboardData.java`** | Keyboard layout data structures (Key, Row, Keyboard). Parses XML layouts into memory and pre-computes bounding boxes and key dimensions. | `load()`, `Key`, `Row`, `Keyboard`, `get_key_at_pos()` |
| **`CurrentlyTypedWord.java`** | Buffer that tracks currently typed word for predictive text and dictionary matching. Preserves local state when editors don't support `getTextBeforeCursor`. | `type_chars()`, `backspace()`, `get_word()`, `set_current_word()` |
| **`EditorConfig.java`** | Inspects `EditorInfo` (`inputType`, `imeOptions`, target package) to configure behavior (e.g. password masking, terminal mode, suggestion bar visibility). | `selection_mode_enabled`, `_should_move_cursor_force_fallback()` |
| **`Config.java`** | App-wide runtime configuration loaded from SharedPreferences (sensitivities, key heights, vibrator settings, gesture scales). | `load()`, `slide_step_px`, `vibration_duration` |
| **`Theme.java` / `ThemeStyle.java`** | Theme engine. Manages fonts, background shapes, colors, stroke widths, key highlight states, and Cyberpunk/Glass styling. | `Theme`, `ThemeStyle`, `Key`, `Label` |
| **`ExtraKeys.java`** | Dynamically attaches secondary/corner characters or accents to keys based on user preferences or language settings. | `apply_extra_keys()`, `parse_extra_keys()` |
| **`KeySvgIcons.java`** | Vector path definitions for functional keyboard symbols (Backspace, Return, Space, Clipboard, Compose). Avoids overriding literal Unicode directional arrow characters (`↑`, `↓`, `←`, `→`, `↖`, `↗`, `↙`, `↘`, `↕`) so they render natively using font glyphs. | `hasIcon()`, `drawIcon()`, SVG path definitions |
| **`ComposeKey.java`** | Implements standard Compose Key sequences (e.g., Compose + ' + e = é). | `apply()`, compose sequence lookup |

---

### Suggestions & Prediction Subsystem (`/app/src/main/java/expected/keyboard2/suggestions/` & `prediction/`)

| File / Directory | Description |
|---|---|
| **`Suggestions.java`** | Connects `KeyEventHandler` to `PredictionEngine`, formats completion candidates, and manages autocorrect state. |
| **`CandidatesView.java`** | Top candidate / suggestion bar view. Renders candidate items, autocorrect highlights (bold), emoji button, and utility bar toggle. |
| **`prediction/PredictionEngine.java`** | Core FUTO prediction engine: prefix search, unigram/bigram scoring, spatial key distance autocorrection, typo mapping, dynamic learning, and emoji prediction. |
| **`prediction/UserDictionary.java`** | On-device personal dictionary & bigram memorization engine with persistent storage and frequency reinforcement. |
| **`prediction/BuiltinLexicon.java`** | Built-in offline corpus containing high-frequency English vocabulary, contractions, bigrams, and common typos. |
| **`prediction/TrieDictionary.java`** | High-speed Trie data structure for prefix matching and frequency retrieval. |
| **`prediction/SpatialDistance.java`** | QWERTY physical key coordinate matrix and weighted Damerau-Levenshtein distance calculation. |
| **`prediction/EmojiPredictor.java`** | Keyword-to-emoji suggestion mapping. |
| **`FloatingKeyboardUtils.java`** | Floating mode controller: top/bottom drag handle bar for moving/repositioning, bottom-right corner resize handle for resizing, and dimension persistence. |

---

### Auxiliary Panes & Dialogs

| File / Package | Description |
|---|---|
| **`EditingPaneView.java`** | Dedicated cursor, select-all, cut, copy, paste, and text navigation keypad overlay. |
| **`ClipboardHistoryView.java`** | Clipboard history manager and pinboard pane. |
| **`EmojiGridView.java`** / **`EmojiGroupButtonsBar.java`** | Categorized emoji picker and scrollable emoji grid. |
| **`CustomLayoutEditDialog.java`** | In-app editor for customizing XML layout files directly on device. |

---

### Settings & Activities (`/app/src/main/java/expected/keyboard2/prefs/`)

| File | Description |
|---|---|
| **`LauncherActivity.java`** | App launcher UI: Enable keyboard, switch input method, quick settings cards, and layout preview. |
| **`SettingsActivity.java`** | Main multi-section settings activity. |
| **`ExtraKeysPreference.java`** | Custom preference dialog to map extra keys to specific keyboard corners. |
| **`LayoutsPreference.java`** | Interface for selecting active layouts and managing custom imported layouts. |

---

## 3. Layout Definitions & XML Layout System (`app/src/main/res/xml/`)

### Layout XML Cardinal Directions Syntax
Every key can define characters/actions triggered by swiping in different directions:
```
       [nw]  [n]  [ne]
        [w]  [c]  [e]
       [sw]  [s]  [se]
```
- **`c`**: Center (normal tap)
- **`nw`**: North-West (flick top-left)
- **`n`**: North (flick up)
- **`ne`**: North-East (flick top-right)
- **`w`**: West (flick left)
- **`e`**: East (flick right)
- **`sw`**: South-West (flick bottom-left)
- **`s`**: South (flick down)
- **`se`**: South-East (flick bottom-right)

### Special Attributes on `<key>`
- **`width="1.5"`**: Relative width of the key (default = 1.0).
- **`shift="0.5"`**: Empty horizontal gap on the left of the key.
- **`role="action"` / `role="space_bar"`**: Defines special functional styling and slider capability.
- **`loc [name]`**: Hidden placeholder slot available for user customization via extra keys.

### Key Layout Files
- **`res/xml/latn_qwerty_us.xml`**: Default standard US QWERTY layout.
- **`res/xml/bottom_row.xml`**: Default bottom row containing Space bar, Ctrl, Alt, Compose, Return.
- **`res/xml/numeric.xml` / `numpad.xml`**: Number pad and symbol keyboard layers.
- **`res/xml/latn_*.xml`**: Language-specific and alternative layouts (Colemak, Dvorak, Neo2, Workman, AZERTY, QWERTZ, etc.).

---

## 4. Key Gestures, Sliders & Terminal Compatibility

### Space Bar & Slider System
1. **Space Bar Swiping (Word Navigation)**:
   - Left swipe (`w="word_left"`) or Right swipe (`e="word_right"`).
   - In standard editors: queries `InputConnection.getTextBeforeCursor` / `getTextAfterCursor`, computes word boundaries, and calls `conn.setSelection()`.
   - In terminal editors (Termux / `InputType.TYPE_NULL`): Automatically invokes `send_ctrl_dpad()` which dispatches synchronized `Ctrl + DPAD_LEFT` and `Ctrl + DPAD_RIGHT` key events.

2. **Cursor Sliders (`Slider.Cursor_left`, `Slider.Cursor_right`, `Slider.Cursor_up`, `Slider.Cursor_down`)**:
   - Activated by swiping and dragging horizontally/vertically.
   - Dispatches continuous step movements scaled by `Config.slide_step_px`.

3. **Delete Word Sliders**:
   - Swiping backspace triggers word/character batch deletion.

---

## 5. Quick Modification Cheat Sheet

| Task | Where to Edit |
|---|---|
| **Change key placement / flick actions** | Edit target XML in `app/src/main/res/xml/` (e.g. `latn_qwerty_us.xml`). |
| **Add/change bottom bar keys** | Edit `app/src/main/res/xml/bottom_row.xml`. |
| **Modify Space bar behavior** | Edit `KeyEventHandler.java` (`handle_space_bar()`) and `EditorConfig.java`. |
| **Modify cursor / terminal navigation** | Edit `KeyEventHandler.java` (`move_cursor()`, `move_cursor_fallback()`, `send_ctrl_dpad()`). |
| **Adjust touch sensitivity / thresholds** | Edit `Config.java` or `Pointers.java` (`step_px`, `min_distance`). |
| **Change theme colors & styling** | Edit `app/src/main/res/values/themes.xml`, `styles.xml`, and `Theme.java`. |
| **Modify suggestion strip / candidate bar** | Edit `CandidatesView.java` and `Suggestions.java`. |
| **Add new special character / action code** | Add enum in `KeyValue.java` and handler in `KeyEventHandler.java`. |
