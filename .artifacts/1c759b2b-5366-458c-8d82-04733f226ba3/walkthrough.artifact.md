# Walkthrough - Google Play In-App Updates Integration

Integrated the official **Google Play In-App Updates API** (`play-app-update-ktx`) so users can manually or automatically check for updates directly from the Google Play Store.

## Changes

### [UpdateManager.kt](file:///C:/Users/USER/Downloads/free-launcher%20(2)/app/src/main/java/com/freelauncher/app/data/service/UpdateManager.kt)
- Created a service wrapping `AppUpdateManagerFactory` to poll the Google Play Store for available app updates using the flexible update pattern.

### [LauncherViewModel.kt](file:///C:/Users/USER/Downloads/free-launcher%20(2)/app/src/main/java/com/freelauncher/app/ui/viewmodel/LauncherViewModel.kt)
- Added `checkForUpdates(context)` which queries `UpdateManager` and surfaces a Toast notification if a newer version is live on the Play Store.

### [AboutSheet.kt](file:///C:/Users/USER/Downloads/free-launcher%20(2)/app/src/main/java/com/freelauncher/app/ui/components/AboutSheet.kt) & [MainActivity.kt](file:///C:/Users/USER/Downloads/free-launcher%20(2)/app/src/main/java/com/freelauncher/app/MainActivity.kt)
- Added a **"Check for Updates"** action item under the Support & Donations section of the **About** sheet.

## Verification Results

### Automated Tests
- Executed `./gradlew assembleDebug` successfully with zero errors.
