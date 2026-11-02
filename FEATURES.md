# FREE Launcher (.noma Launcher) — Comprehensive Details & Feature Specifications

**FREE Launcher** (also known as `.noma Launcher`) is a modern, minimalist, open-source Android home screen launcher built using **Kotlin**, **Jetpack Compose**, **Material 3**, and **Coroutines Flow**. It is engineered specifically to help users reduce digital distractions, cultivate mindful phone usage, and enjoy a clean, fast, and highly customizable mobile experience.

---

## Table of Contents
1. [Core Philosophy](#1-core-philosophy)
2. [Spatial Navigation & Gesture Map](#2-spatial-navigation--gesture-map)
3. [Screens & Core Interfaces](#3-screens--core-interfaces)
   - [Home Screen](#home-screen)
   - [Six Apps View (Quick Access)](#six-apps-view-quick-access)
   - [All Apps Drawer (Classic & Neo Minimal)](#all-apps-drawer-classic--neo-minimal)
   - [RSS News Feed Screen](#rss-news-feed-screen)
   - [Time Away Screen (Digital Wellbeing)](#time-away-screen-digital-wellbeing)
4. [Mindful Control & Focus Tools](#4-mindful-control--focus-tools)
5. [Aesthetics & Customization](#5-aesthetics--customization)
6. [Integrated Utilities & Features](#6-integrated-utilities--features)
7. [Technical Architecture & Tech Stack](#7-technical-architecture--tech-stack)

---

## 1. Core Philosophy

- **Intentional Phone Usage**: Encourages users to pause before opening mindless apps and stay present.
- **Pure Minimalism**: Eliminates app clutter, flashy notifications, and addictive home screen grid layouts.
- **Fast & Lightweight**: Zero bloated ads or trackers, powered by reactive StateFlow and modern Android Architecture Components.
- **Private & Local-First**: Works 100% offline; backup files, settings, and RSS cache stay local on your device.

---

## 2. Spatial Navigation & Gesture Map

FREE Launcher uses an intuitive **spatial direction map** for seamless one-handed navigation without needing on-screen buttons:

| Gesture | Action |
| :--- | :--- |
| **Swipe Up** | Open **Six Apps (Quick Access)** view |
| **Swipe Down** | Open **Universal Search** immediately |
| **Swipe Left** | Navigate to the **RSS News Feed Screen** |
| **Swipe Right** | Navigate to the **Time Away (Digital Wellbeing)** screen |
| **Double-Tap Background** | Turn off / lock display (via `NomaAccessibilityService`) |
| **Triple-Tap Background** | Toggle **UltraFocus Mode** (lock to pinned apps only) |
| **Long-Press Background** | Open **Settings & Customization Sheet** |
| **Long-Press Time / Clock** | Enter **Clock Edit & Drag-to-Place Mode** |

---

## 3. Screens & Core Interfaces

### Home Screen
- **Minimalist Clock Widget**: Choice of 16 distinct clock styles (Digital, Stacked, Word-based, Serif, Rotating Wheels, 3D Bubbly, Flip, etc.).
- **Interactive Drag-to-Place**: Touch and drag the clock card anywhere on screen with exact position persistent saving and scale adjustments (50% to 200%).
- **Date Display**: Formatted current date with tap shortcut to system Calendar.
- **Contextual Greeting**: Automatic time-based greeting (*"Good morning"*, *"Good afternoon"*, *"Good evening"*, *"Stay present"*) or user-defined custom phrase.
- **Phone-Free Today Badge**: Quick indicator showing hours/minutes spent away from phone today.
- **Weather & Battery Glance**: Text-only status badge showing current temperature (°F / °C), weather condition, and battery level / charging state (`⚡ 85%`).
- **Quick Action Buttons**: Discreet Lens Search icon and Phone Dialer icon.
- **Hide Everything Mode**: Clean canvas toggle to hide clock, date, and widgets completely.

### Six Apps View (Quick Access)
- **Top 6 Pinned Apps**: Displays up to 6 priority apps on home view.
- **Custom Scaling**: Adjustable scale slider (60% to 180%).
- **Alignment Modes**: Align list to Left, Center, or Right.
- **Icon / Monogram / Text Display**: Choose between pure text labels, minimal monograms, or app icons.
- **Pin Management**: Pin/Unpin, reorder via drag dialog, or assign multi-apps.

### All Apps Drawer (Classic & Neo Minimal)
- **Dual Drawer Styles**:
  1. **Classic**: Text list organized by top category tabs.
  2. **Neo Minimal (Beta)**: Centered text list with swipe categories and floating search bar.
- **Smart App Categorization**: 18 standard categories (*Communication, Social, Productivity, Work, Finance, Shopping, Media, Photography, Travel, Food, Utilities, Tools, News, Games, Health & Fitness, Education, Entertainment, System*).
- **Category Manager**: Add custom categories, rename, hide/unhide, or reorder categories.
- **Work Profile / Dual Apps Support**: Multi-user serial number launcher support for work profile apps.
- **Alphabetical Fast Scroll & Filtering**: Instant search filtering as you type.

### RSS News Feed Screen
- **Offline-First RSS Reader**: Aggregates news from custom RSS XML/Atom feeds powered by Room DB.
- **Category Grouping**: Organize news sources by topic (e.g., *Tech, World News, Science*).
- **In-App Article Preview**: Read full descriptions or open links in system browser.
- **Auto-Syncing**: Automatic background fetching with feed management dialog to add/remove custom RSS URLs.

### Time Away Screen (Digital Wellbeing)
- **Mindful Usage Dashboard**: Tracks phone usage vs. time away.
- **Weekly Focus Graph**: Visual bar chart of phone-free hours per day.
- **Focus Sessions & Mindful Unlocks**: Logs focus sessions and intentional unlocks.
- **Built-in Focus Timer**: Integrated Pomodoro-style timer (e.g., 25-minute focus blocks) to encourage uninterrupted deep work.

---

## 4. Mindful Control & Focus Tools

1. **Mindful Pause for Distracting Apps**
   - Forces a 5-second breathing pause before launching social, gaming, media, or shopping apps. Gives users time to reconsider impulsive app opening.
2. **UltraFocus Mode (Pin Lock)**
   - Lock navigation strictly to the 6 pinned apps. Blocks access to drawer, settings, and other apps. Exit requires a triple-tap gesture.
3. **Double-Tap to Sleep**
   - Built-in `NomaAccessibilityService` lets users double-tap empty space on the home screen to turn off and lock the device display immediately. Includes Android-compliant prominent disclosure modal.
4. **App Lock Protection (PIN & Biometrics)**
   - Protect access to the All Apps drawer and Settings Sheet with either a custom 4-digit secret PIN or System Biometrics (Fingerprint / Face Unlock).

---

## 5. Aesthetics & Customization

- **Clock Styles (16 Variants)**:
  - Large Digital, Thin Digital, Monospaced `[10:16]`, Minimal Stacked, Word-Based Clock, Compact Digital, Elegant Serif, Minimal Dot-Based, Handwritten, Modern Outline, Bold Block, Minimal w/ Seconds, Bubbly 3D, Flip Split-Flap, Timeline Progress, Rotating Wheels.
- **Color Schemes & Themes**:
  - **OLED Void Black**: Pitch black background for maximum battery savings on OLED screens.
  - **Material You**: Dynamic palette extracted from wallpaper colors.
  - **Charcoal**: Dark sleek neutral surface.
  - **Paper Warm**: Soft, eye-friendly light warm tone.
  - **Minimal Light**: Clean, bright white UI.
- **Wallpapers & Gradient Studio**:
  - Built-in atmospheric wallpaper gradients (*Cyber Noir, Solar Flare, Midnight Nebula, Deep Sea, Emerald Forest, Sunset Gold, Minimal Slate, Crimson Dusk*).
  - **Atmospheric Theme Creator**: Create and save custom gradient wallpapers to Room database.
  - **Device Gallery Photo Picker**: Select personal device photos as launcher wallpaper.
  - **Wallpaper Dimming**: Adjustable wallpaper dimming overlay (0% to 100%).
- **Typography & Fonts**:
  - Choice of custom launcher fonts (*Minimal Sans, Elegant Serif, Monospace, Condensed, System Default*).
- **App Icons & Third-Party Packs**:
  - **Text-Only Mode**: Clean text labels for a distraction-free experience.
  - **Monogram Mode**: Custom circular letter badges.
  - **System Icons**: Official app icons.
  - **Third-Party Icon Pack Support**: Compatible with Nova/Apex icon packs.
  - **Minimal Icon Pack Creator**: Built-in generator to customize monochrome shapes, colors, and borders for all installed apps.

---

## 6. Integrated Utilities & Features

- **Universal Search**: Fast search indexing for apps, system settings, and web shortcuts.
- **Weather & Battery Integration**: Integrated `WeatherService` querying Open-Meteo API for real-time local weather (°F/°C) without tracking location unnecessarily, alongside live battery broadcast receiver.
- **Backup & Restore**: Export all launcher preferences, pinned apps, categories, font, and theme settings as a structured JSON file; import anytime to restore your setup.
- **Landscape Mode Support**: Forces sensor landscape or adapts automatically to tablet and foldables.
- **Haptic Engine (`LauncherHaptics`)**: Crisp haptic feedback for gestures, buttons, drag-and-place, and slider interactions.

---

## 7. Technical Architecture & Tech Stack

| Component | Technology / Library |
| :--- | :--- |
| **Language** | Kotlin (100%) |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Architecture Pattern** | MVVM with Reactive `StateFlow` |
| **Local Database** | Room ORM (Notes, Calendar Events, RSS Feeds & Articles, Focus Sessions, Wallpapers, Settings) |
| **Asynchronous Logic** | Kotlin Coroutines & Flow |
| **Image Loading** | Coil Compose |
| **Accessibility Service** | `NomaAccessibilityService` (for Double-Tap Screen Lock) |
| **Hardware Integration** | BiometricPrompt API, Android UsageStatsManager, BatteryManager BroadcastReceiver |
| **Build Tooling** | Gradle Kotlin DSL (`build.gradle.kts`) |
| **Unit & UI Testing** | JUnit, Robolectric, Screenshot Testing |

---

*Generated for FREE Launcher (.noma Launcher) codebase analysis.*
