# Walkthrough - Custom Gallery Wallpaper Immediate Apply (Cache-Busting Fix)

Fixed the bug where changing the custom gallery wallpaper would only apply after deleting the previous image file.

## Changes Made

### 1. Unique Timestamped Filenames & Cleanup (`LauncherViewModel.kt`)
- Updated `saveCustomWallpaperFromUri`:
  - Instead of overwriting a static filename (`custom_wallpaper_image.jpg`), each new custom gallery image is now saved with a unique timestamp (`custom_wallpaper_${System.currentTimeMillis()}.jpg`).
  - Automatically cleans up older custom wallpaper files in `context.filesDir` to keep local storage clean.

### 2. Coil Image Cache-Busting (`MainActivity.kt`)
- Updated `AsyncImage` model in [MainActivity.kt](file:///C:/Users/USER/Downloads/free-launcher%20(2)/app/src/main/java/com/freelauncher/app/MainActivity.kt#L105-L125):
  - Wrapped model data in an `ImageRequest.Builder` specifying explicit `memoryCacheKey` and `diskCacheKey` bound to the unique `customWallpaperUri`.
  - Guarantees Coil immediately detects the new file path and renders the new wallpaper without serving cached old images.

## Verification Results
- Ran `:app:assembleDebug` via `gradle_build` -> **SUCCESS**.
- Static analysis -> **0 errors**.
