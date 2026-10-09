# YT Browser (Android starter)

A light-themed Android WebView browser starter with Google search, bookmarks, download handling, HTTPS-only download enforcement, third-party-cookie restriction, mixed-content blocking, certificate-error rejection, and a small built-in ad/tracker host filter.

## Requirements
- Android Studio (current stable)
- Android SDK Platform 35
- JDK 17 (Android Studio's bundled JDK is fine)
- Android device/emulator running Android 8.0 (API 26) or later

## Open and run
1. Unzip `YTBrowser.zip`.
2. In Android Studio choose **Open** and select the `YTBrowser` folder.
3. Allow Gradle sync to finish; install SDK Platform 35 if prompted.
4. Connect an Android phone with USB debugging enabled, or start an emulator.
5. Press **Run**.

## What works
- Google search / URL bar, back, home, bookmarks.
- WebView browsing and basic ad/tracker host blocking.
- Downloads initiated by a site, saved in the device Downloads folder through Android DownloadManager.
- HTTPS certificate errors are rejected, mixed content is blocked, third-party cookies are disabled.

## Honest limitations
- This is a starter implementation, not a production-hardened browser. The blocklist is intentionally small and does not block all ads; YouTube and YouTube Music ads may still appear. Blocking can also break some sites.
- The starter uses one WebView, so it does **not** yet implement real multi-tab browsing, sync, advanced safe-browsing reputation checks, or a full browser-grade ad-block engine.
- Google search is loaded as a website; this is not an official Google browser.
- YouTube content stays in YouTube's official web experience. The app does not extract or download streams from YouTube/YouTube Music. Use official offline features or direct audio files where the rights holder permits downloading.
- Only download files you have permission to save. Some sites may require authentication and won't expose a direct downloadable URL.

## Suggested next steps for a production release
1. Add Android Safe Browsing support and a maintained, regularly updated filter list with transparent update policy.
2. Implement actual tabs using a WebView per tab and lifecycle management.
3. Add bookmark/history persistence with Room/DataStore and a clear-data screen.
4. Add automated tests, privacy policy, accessibility review, and security review.
5. Configure app signing and test on a range of Android versions.

## Project structure
- `app/src/main/AndroidManifest.xml` — permissions and activity declaration
- `app/src/main/java/com/yt/browser/MainActivity.kt` — browser UI and behavior
- `app/build.gradle.kts` — Android build configuration
