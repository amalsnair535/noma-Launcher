# NOMA Launcher (.noma) — Version 4.2 Release Notes

**Version 4.2 (Version Code 30)**  
*Target SDK: 37 (Android 15+)  | Min SDK: 24 (Android 7.0+)*

---

## 🌟 What's New in Version 4.2

NOMA Launcher 4.2 introduces **Direct Home Screen Scribble to Search**, **Windows Phone Metro Style Typography** in the Neo Minimal Drawer, **Android 15 (16 KB page boundary) compliance**, and major gesture and settings hub enhancements.

---

### ✍️ 1. Direct Home Screen Scribble to Search (Google ML Kit Integration)

- **On-Device Handwriting Recognition**: Draw any letter or word anywhere on the Home screen to filter installed apps, system settings, and web search instantly using Google ML Kit Digital Ink Recognition. 100% on-device and private.
- **Fast Single-Letter Recognition (180ms)**: Optimized stroke recognition delay to 180ms. Drawing single letters (*"c"*, *"i"*, *"w"*, *"m"*) launches matching apps without waiting.
- **Drawer-Matched Results View**: Search results automatically adapt to your active App Drawer style (**Classic** or **Neo Minimal**).
- **Clean Results Canvas (No Search Bar / No Keyboard)**: When opened via Scribble Search, the search text box and soft keyboard are hidden for an uncluttered full-page view of matching apps.
- **Explicit Settings Disclosure & Default OFF**: Scribble Search is turned OFF by default. Turning it ON displays a confirmation popup that automatically disables *Swipe Down to Search* to eliminate gesture conflicts.

---

### 🎨 2. Windows Phone (Metro UI) Style Neo Minimal Drawer

- **Large 32sp Metro Typography**: Upgraded category headers in Neo Minimal App Drawer to large 32sp Windows Phone Metro style typography (*all apps*, *social*, *media*, *work*, *games*).
- **App Count Subtitle**: Displays app count (e.g., *32 apps*) in clean secondary typography underneath the category title.
- **Always-Visible Headers**: Category headers remain visible at the top as you swipe left/right between category pages.

---

### 🖐️ 3. Gesture System & Disambiguation Enhancements

- **Edge-Restricted Horizontal Swipes**: Swiping left for RSS News Feed and swiping right for Time Away Focus Screen strictly requires starting near horizontal screen edges (outer 38%). Drawing multi-stroke letters like *"A"*, *"H"*, *"E"*, or *"Z"* on the main canvas will never trigger page navigation.
- **Letter *"i"* vs *"l"* Matcher**: `InstalledAppMatcher` expands single-character vertical stroke queries (*"l"*, *"1"*, *"i"*, *"I"*) to search both *"i"* and *"l"* starting apps (Instagram, Internet, Clock, Contacts, Gallery).
- **Reliable Tap & Long-Press Passthrough**: Added touch displacement filters (> 12px) and drawing state gating (`lastScribbleTime`) so Double-Tap to Sleep and Long-Press to Settings work 100% reliably while Scribble is enabled.

---

### ⚙️ 4. Categorized Settings Sheet & Support Section

- **Full-Screen Categorized Hub**: Reorganized Settings Sheet into distinct categories (*Appearance*, *Apps & Icons*, *Home Screen*, *Focus & Privacy*, *Feeds & System*).
- **Help & Support Hub**: Added main index links for *Onboarding & Gesture Guide* and *About NOMA Launcher & Support*.
- **Hide Everything on Home**: Added a one-tap toggle to hide the clock, date, and widgets for a completely blank home screen canvas.

---

### 🔒 5. Android 15 & Google Play 16 KB Page Size Compliance

- **Target SDK 37 (Android 15+)**: Updated build target to Android 15 (SDK 37).
- **16 KB Native Page Alignment**: Added `packaging.jniLibs.useLegacyPackaging = false` and `android.bundle.enableUncompressedNativeLibs = true` to ensure native JNI libraries (`libdigitalink.so`) comply with Google Play's 2025 16 KB page boundary requirements.
- **Store Metadata Policy Resolution**: Resolved Play Console metadata warnings in `PLAY_STORE_METADATA.md` with policy-compliant store title, short description, and full description.

---

## 🛠️ Bug Fixes & Performance Improvements

- **Fixed Keyboard Popup on Exit**: Resolved an issue where exiting Scribble Search Results caused the soft keyboard to pop up during screen transitions.
- **Fixed Settings Opening While Scribbling**: Updated `onLongClick` and touch move listeners to prevent stationary long-press timers from firing while drawing strokes.
- **Fixed System Wallpaper Sync**: Added `ACTION_WALLPAPER_CHANGED` broadcast receiver and `FLAG_SHOW_WALLPAPER` window flags in `MainActivity.kt`.
- **Fixed About Sheet Callback**: Corrected callback wiring for opening the About Sheet from Settings.
- **Memory & Scroll Haptics Optimization**: Reduced state allocations during fast scrolling in All Apps and Neo Minimal drawers.

---

## 📋 Version Summary (4.0 → 4.2)

| Version | Highlights |
| :--- | :--- |
| **v4.0** | Initial Release — Minimalist Clock, Six Apps, Classic & Neo Minimal Drawer, RSS Reader, Time Away, Mindful Pause, OLED Void Theme |
| **v4.1** | Category Manager, Custom Wallpapers, Atmospheric Gradient Studio, Backup & Restore, Biometric & PIN Lock |
| **v4.2** | Direct Scribble to Search (ML Kit), Windows Phone Metro Typography, 16 KB Page Alignment (Android 15+), Settings Hub Redesign, Gesture Passthrough Fixes |

---

*NOMA Launcher v4.2 — Built for intentional phone usage, speed, and privacy.*
