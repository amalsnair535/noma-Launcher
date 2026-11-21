# Google Play In-App Updates Integration

This plan outlines the integration of the **Google Play In-App Updates API** to automatically check for updates available on the Google Play Store and prompt the user to update (either via an immediate flow or a flexible background update with a notification).

## User Review Required

> [!IMPORTANT]
> The Google Play In-App Updates API requires the app to be downloaded/signed via Google Play to test fully in production, but supports testing via internal test tracks or debug/test harnesses. We will use the **Flexible Update** flow so a clean update banner/dialog prompts the user when an update is detected.

## Proposed Changes

### [MODIFY] [libs.versions.toml](file:///C:/Users/USER/Downloads/free-launcher%20(2)/gradle/libs.versions.toml)
- Add `appUpdate` and `appUpdateKtx` library dependencies for Google Play In-App Updates.

### [MODIFY] [build.gradle.kts](file:///C:/Users/USER/Downloads/free-launcher%20(2)/app/build.gradle.kts)
- Add `implementation(libs.play.app.update)` and `implementation(libs.play.app.update.ktx)`.

### [NEW] [UpdateManager.kt](file:///C:/Users/USER/Downloads/free-launcher%20(2)/app/src/main/java/com/freelauncher/app/data/service/UpdateManager.kt)
- Create a manager using `AppUpdateManagerFactory` to check for available updates on Google Play.
- Provide methods to start a flexible update or check version availability on app launch or via settings.

### [MODIFY] [LauncherViewModel.kt](file:///C:/Users/USER/Downloads/free-launcher%20(2)/app/src/main/java/com/freelauncher/app/ui/viewmodel/LauncherViewModel.kt)
- Add a method `checkForPlayStoreUpdate(context: Context)` to query the `AppUpdateManager`.
- Add state flags for update availability.

### [MODIFY] [SettingsSheet.kt](file:///C:/Users/USER/Downloads/free-launcher%20(2)/app/src/main/java/com/freelauncher/app/ui/components/SettingsSheet.kt)
- Add a "Check for Play Store Updates" button under the About section.

## Verification Plan

### Automated Tests
- Build test via `./gradlew assembleDebug` to verify dependency resolution and compilation.

### Manual Verification
- Tap "Check for Updates" in Settings (will query Play Store services gracefully).