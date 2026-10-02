# App Icon for Google Play Store

The Google Play Store requires a **512x512 PNG** image. Since the "Export" option isn't appearing in your Resource Manager, please use the **Image Asset Studio**—it's the most reliable way to generate the correct file.

## Recommended Method: Image Asset Studio

This method will automatically combine your background and foreground into a single 512x512 PNG named `ic_launcher-web.png`.

1.  **Right-click the `res` folder** in your Project view (on the left side of Android Studio, not in the Resource Manager).
2.  Select **`New > Image Asset`**.
3.  **Configure the layers**:
    *   **Icon Type**: Ensure it is set to "Launcher Icons (Adaptive and Legacy)".
    *   **Name**: Keep it as `ic_launcher`.
    *   **Foreground Layer tab**: Ensure the "Path" points to your `ic_launcher_foreground.xml` (or it may already be selected).
    *   **Background Layer tab**: Ensure it points to your `ic_launcher_background.xml` (or a solid color if you prefer).
4.  **Scaling**: Use the "Resize" slider to make sure the icon fits nicely within the safe zone (the black circle in the preview).
5.  Click **Next**.
6.  **Find the file**: Look at the "Output Directories" list. You will see a file listed as **`ic_launcher-web.png`**.
7.  Click **Finish**.

### Where to find the 512x512 file:
After clicking Finish, the file will be created here:
`app/src/main/ic_launcher-web.png`

You can right-click that file in the Project tree and select **"Open in Explorer"** to get the actual PNG to upload to the Play Console.

## Why "Export" was missing:
Android Studio often hides the "Export" option for Vector Drawables (XML) in the Resource Manager because they are code, not images. The **Image Asset Studio** (Step 1 above) is the correct tool for converting these vectors into the final PNGs required by the store.
