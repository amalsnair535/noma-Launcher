# Implementation Plan - Time Away Screen

The goal is to implement a new "Time Away" screen accessible via a right-swipe gesture from the Home screen. This screen will display real Digital Wellbeing statistics from the device, helping users track their "offline" time and reclaimed productivity.

## Proposed Changes

### Data Layer

#### [MODIFY] [DigitalWellbeingService.kt](file:///C:/Users/USER/Downloads/free-launcher%20(2)/app/src/main/java/com/freelauncher/app/data/service/DigitalWellbeingService.kt)
- Add `TimeAwayStats` data class to represent the new metrics.
- Implement `getTimeAwayStats()` to calculate:
    - **Today's Phone-Free Time**: Total elapsed time today minus active screen time.
    - **Longest Break**: The longest continuous period where the screen was off today.
    - **Current Break**: The time elapsed since the user last turned off their screen.
    - **Reclaimed Time**: Total phone-free time for the current week.
    - **Weekly History**: Phone-free percentages for the last 7 days.

### ViewModel Layer

#### [MODIFY] [LauncherViewModel.kt](file:///C:/Users/USER/Downloads/free-launcher%20(2)/app/src/main/java/com/freelauncher/app/ui/viewmodel/LauncherViewModel.kt)
- Add `TIME_AWAY` to the `LauncherScreen` enum.
- Add `timeAwayStats: TimeAwayStats?` to `LauncherUiState`.
- Update `refreshDigitalWellbeingStats()` to populate the new state using the service.

### UI Layer

#### [NEW] [TimeAwayScreen.kt](file:///C:/Users/USER/Downloads/free-launcher%20(2)/app/src/main/java/com/freelauncher/app/ui/screens/TimeAwayScreen.kt)
- Create a minimalist, atmospheric screen based on the provided design.
- Use `MaterialTheme.colorScheme` and project design principles (Frosted glass effects, OLED-friendly blacks).
- Components:
    - **Header**: Back button and title.
    - **Today Hero Card**: Shows total phone-free time today.
    - **Stat Grid**: Longest break vs. Current break.
    - **Percentage Card**: Today's phone-free ratio with a progress bar.
    - **Weekly Chart Card**: Horizontal bars showing the last 7 days.
    - **Insights Grid**: Longest breaks history and Total weekly reclaimed time.

### Navigation

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/USER/Downloads/free-launcher%20(2)/app/src/main/java/com/freelauncher/app/ui/screens/HomeScreen.kt)
- Add a swipe gesture (drag finger right) to navigate to `TIME_AWAY`.
- Add a subtle left edge hint bar (similar to the news feed hint) for the new screen.

#### [MODIFY] [SixAppsView.kt](file:///C:/Users/USER/Downloads/free-launcher%20(2)/app/src/main/java/com/freelauncher/app/ui/screens/SixAppsView.kt)
- Add a drag gesture to navigate to `TIME_AWAY`.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/USER/Downloads/free-launcher%20(2)/app/src/main/java/com/freelauncher/app/MainActivity.kt)
- Add `LauncherScreen.TIME_AWAY` to the navigation `AnimatedContent`.
- Define slide-in/out transitions for the new screen (entering from left).

## Verification Plan

### Automated Tests
- Build project: `./gradlew :app:assembleDebug`.

### Manual Verification
- **Gesture Test**: Swipe right on Home screen; verify `TimeAwayScreen` slides in from the left.
- **Data Test**: Grant usage permissions and verify that "Today's Time Away" updates correctly after using and locking the phone.
- **Visual Test**: Verify the UI uses the app's current theme colors and is safe from camera punch-hole overlaps.
