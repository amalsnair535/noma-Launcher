# Privacy Policy for .noma Launcher

**Last Updated: October 3, 2026**

.noma Launcher ("we," "us," or "our") is committed to protecting your privacy. This Privacy Policy explains how we collect, use, and safeguard your information when you use our Android application.

## 1. Core Privacy Principle
.noma Launcher is designed to be a "Privacy-First" application. **We do not sell, rent, or trade your personal data.** Almost all data processed by the app stays locally on your device.

## 2. Information Collection and Use

### 2.1 Sensitive Device Permissions
To provide its core functionality as a minimalist launcher and productivity tool, .noma Launcher requests the following sensitive permissions:

*   **QUERY_ALL_PACKAGES (All Apps Access)**: 
    *   **Purpose**: Essential for a launcher to list, categorize, and open the applications installed on your device.
    *   **Data Handling**: The list of your installed apps is used only to populate the home screen and app drawer. This data is **never** transmitted off your device.
*   **PACKAGE_USAGE_STATS (Usage Access)**:
    *   **Purpose**: Power the "Digital Wellbeing" and "Productivity" features, allowing you to see your app usage history and set focus goals.
    *   **Data Handling**: This data is read directly from the Android system and processed locally. We do not store or transmit your usage patterns.
*   **ACCESS_COARSE_LOCATION / ACCESS_FINE_LOCATION**:
    *   **Purpose**: Enables the live Weather & Battery Glance feature by obtaining your approximate or precise location to fetch regional weather.
    *   **Data Handling**: Location coordinates are sent securely to the Open-Meteo weather API solely to retrieve current weather conditions. If location permissions are denied, the app can fall back to IP-based geolocation. Location data is never stored on external servers or sold.
*   **READ_CONTACTS**:
    *   **Purpose**: Enables "Universal Search" to find and display your contacts for quick dialing or messaging.
    *   **Data Handling**: Contact details are accessed only during an active search and are not stored in our database or sent to any server.
*   **READ_SMS**:
    *   **Purpose**: Allows "Universal Search" to find and display text messages locally on your device.
    *   **Data Handling**: Message content is read in real-time to provide search results. No message data is ever uploaded or shared.
*   **USE_BIOMETRIC / USE_FINGERPRINT**:
    *   **Purpose**: Provides an optional security layer (App Lock) for accessing protected screens and features.
    *   **Data Handling**: We do not have access to your actual biometric data (fingerprints/face). We only receive a "success" or "failure" token from the Android system.

### 2.2 Other Permissions
*   **INTERNET**: Used to fetch live weather forecasts, RSS feeds (News) from URLs you provide, perform web searches, and connect to Firebase services.
*   **VIBRATE**: Used for haptic feedback during UI interactions to improve the user experience.

## 3. Data Storage
.noma Launcher uses a local **Room Database** to store:
*   Notes you create.
*   Calendar events you add.
*   Custom app categories and reordering preferences.
*   Launcher settings (wallpaper ID, font choice, weather/glance preferences, etc.).

**Note**: This database is stored in your device's private storage. If you uninstall the app, this data is deleted by the Android system.

## 4. Third-Party Services
We use trusted third-party services to ensure app stability and functionality:
*   **Firebase Crashlytics & Analytics**: Collects anonymous crash reports, performance metrics, and usage analytics to help us diagnose bugs and improve app reliability.
*   **Open-Meteo API**: Used for fetching weather forecasts based on regional coordinates.
*   **Firebase App Check**: Ensures the integrity of the application and prevents unauthorized access to internal services.

## 5. Data Retention and Deletion
Since your personal data is stored locally, you have full control over it.
*   **Notes/Events**: You can delete these individually within the app.
*   **Permissions**: You can revoke any permission at any time through the **Android System Settings**.
*   **Full Reset**: Clearing the app's cache and data in System Settings will delete all local launcher data.

## 6. Changes to This Policy
We may update our Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy on this page and updating the "Last Updated" date.

## 7. Contact Us
If you have any questions or suggestions about our Privacy Policy, do not hesitate to contact us at:
**Email**: amalsnair535@gmail.com
