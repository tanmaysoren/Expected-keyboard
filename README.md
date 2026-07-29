# Expected Keyboard

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com)
[![Built from scratch](https://img.shields.io/badge/built-from%20scratch-orange)](#)

**Expected Keyboard** is a minimal, privacy-first Android keyboard built from scratch. 

As the name suggests: it does exactly what you expect a keyboard to do. No surprises, no tracking, no internet access. Just fast, reliable typing.

### **Screenshots**

<img width="720" height="1604" alt="Screenshot_2026-07-12-19-02-39-26_820944be9aa1afa44ff6705854aac58c" src="https://github.com/user-attachments/assets/1d466738-c656-49e7-8de2-cf5cbb12dc01" />




### **Why Expected Keyboard?**

Inspired by the minimalism of Unexpected Keyboard, but rewritten 100% from scratch to be more predictable and customizable. 

**The idea**: Keyboards shouldn't be unexpected. They should be fast, private, and get out of your way.

### **Features**
- **100% Offline**: No `INTERNET` permission. Your keystrokes physically cannot leave your device
- **Ultra Lightweight**: <12MB APK, zero battery drain in background  
- **Built for Speed**: Compact layouts, swipe gestures, quick symbol access
- **Programmer Friendly**: Easy access to `Tab`, `Ctrl`, `Alt`, arrows, and all symbols
- **Fully Customizable**: [Add what you made customizable - key height, layouts, themes, etc]
- **No Bloat**: No emoji search, no GIFs, no cloud, no accounts

### **Privacy First**
Expected Keyboard does not collect, store, or transmit any data. Period. 

| Permission | Why we need it |
| --- | --- |
| `VIBRATE` | Optional haptic feedback on keypress |
|  ` DOA `  | display over other apps for floating keyboard. |
That's the full list. See [PRIVACY.md](PRIVACY.md) for details.

Android will warn you that keyboards can "collect all text you type". That's true for any keyboard. This app is open source so you can verify it doesn't. Or build it yourself.

### **Installation**
1. Download the latest `.apk` from [Releases]
2. Install → Open app → Tap "Enable Expected Keyboard"
3. Switch to it: Tap the keyboard icon in your nav bar while typing.
   
### **Keys*

SysRq → ⎚

Home → ⇱

Selectall → ☑

PrtScn → ⎙

End → ⇲

Ins → ⎀

PgUp → ⇞

PgDn → ⇟

Copy → ⎘

Paste→ ⎗

Tab → ⇋

Clipboard → ⧉

`Pg up and pg down on p key`

Build from source:
```bash
git clone https://github.com/yourusername/expected-keyboard.git
cd expected-keyboard
./gradlew installDebug.
