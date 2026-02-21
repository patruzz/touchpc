# TouchPC - Google Play Store Release Checklist

**Package:** com.touchpc.remotecontrol
**Version:** 1.0.0

Use this checklist to track all steps required to publish TouchPC to the Google Play Store.

---

## 1. Prerequisites

- [ ] **Google Play Developer Account** - Register at https://play.google.com/console/ (one-time fee of $25 USD). Account review may take up to 48 hours.
- [ ] **Google account identity verification** - Complete identity verification as required by Google (government-issued ID or business documentation).

---

## 2. App Signing and Build

- [ ] **Generate release keystore** - Create a keystore file for signing the app. Store it securely and never commit it to version control. Back up the keystore and its credentials in a safe location -- losing the keystore means you cannot update the app.
- [ ] **Configure keystore.properties** - Ensure `keystore.properties` is configured with the correct `storeFile`, `storePassword`, `keyAlias`, and `keyPassword` values.
- [ ] **Build release AAB** - Generate the release Android App Bundle (AAB) by running:
  ```
  ./gradlew bundleRelease
  ```
  Output location: `app/build/outputs/bundle/release/app-release.aab`
- [ ] **Test the release build** - Install and test the release APK on a physical device to verify that ProGuard/R8 minification has not broken any functionality:
  ```
  ./gradlew assembleRelease
  adb install app/build/outputs/apk/release/app-release.apk
  ```
- [ ] **Opt into Google Play App Signing** - When uploading your first AAB, Google Play will manage your app signing key. You will upload your upload key, and Google will re-sign the app with a Google-managed key. This is mandatory for new apps.

---

## 3. Store Listing Content

### 3.1 Text Content

- [ ] **App name** - "TouchPC - Remote Control" (English) / "TouchPC - Control Remoto" (Spanish). Max 30 characters.
- [ ] **Short description** - Max 80 characters. See `listing-en.md` and `listing-es.md`.
- [ ] **Full description** - Max 4000 characters. See `listing-en.md` and `listing-es.md`.
- [ ] **Category** - Select "Tools" in the Play Console.
- [ ] **Tags** - Add relevant tags from the keywords listed in the listing files.

### 3.2 Graphic Assets

- [ ] **App icon** - 512 x 512 px, PNG, 32-bit, up to 1 MB. Must match the launcher icon.
- [ ] **Feature graphic** - 1024 x 500 px, PNG or JPEG. Displayed at the top of the store listing. Create a promotional banner showing the app's touchpad and keyboard functionality.
- [ ] **Phone screenshots** - Minimum 2, maximum 8. Dimensions: 16:9 or 9:16 aspect ratio, minimum 320 px, maximum 3840 px on any side. Recommended screenshots:
  1. Connection screen - showing the server entry and connection flow.
  2. Touchpad screen - showing the virtual touchpad in use.
  3. Keyboard screen - showing the QWERTY keyboard layout.
  4. Shortcuts screen - showing the quick shortcuts grid.
  5. Settings screen - showing the customization options.
- [ ] **7-inch tablet screenshots** (optional but recommended) - Same requirements as phone screenshots but taken on a 7-inch tablet or emulator.
- [ ] **10-inch tablet screenshots** (optional but recommended) - Same requirements as phone screenshots but taken on a 10-inch tablet or emulator.

### 3.3 Video (Optional)

- [ ] **Promo video** - YouTube URL showing app functionality. Recommended length: 30 seconds to 2 minutes. Not required but improves conversion rate.

---

## 4. Privacy and Compliance

- [ ] **Privacy policy** - Host the privacy policy (see `privacy-policy.md`) at a publicly accessible URL. Options:
  - GitHub Pages (free).
  - A dedicated page on your website.
  - Google Sites (free).
  The URL must be accessible without authentication.
- [ ] **Enter privacy policy URL** in the Play Console under "App content" > "Privacy policy".
- [ ] **Data safety form** - Complete the Data Safety section in the Play Console. For TouchPC, declare:
  - No data is collected.
  - No data is shared with third parties.
  - Data is not encrypted in transit (local network only; or mark as encrypted if you add TLS later).
  - Users cannot request data deletion (no data is collected).
- [ ] **Ads declaration** - Declare that the app does NOT contain ads.

---

## 5. Content Rating

- [ ] **Complete the content rating questionnaire** in the Play Console under "App content" > "Content rating". Answer all questions honestly:
  - No violence, sexual content, or offensive language.
  - No user-generated content.
  - No gambling.
  - No controlled substance references.
  - Expected rating: IARC "Everyone" / PEGI 3 / ESRB E.
- [ ] **Review the assigned rating** and confirm it matches expectations.

---

## 6. Target Audience and App Access

- [ ] **Target audience** - Select "13 and above" or "All ages" depending on your preference. Since the app does not collect data and has no objectionable content, it qualifies for all age groups.
- [ ] **App access** - If the app requires the TouchPC PC server to function, provide instructions to the review team:
  - Explain that the app requires a companion PC server on the same local network.
  - Provide a download link for the PC server or indicate that limited functionality can be demonstrated without it (connection screen is still accessible).
- [ ] **News apps declaration** - Declare that this is NOT a news app.
- [ ] **Government apps declaration** - Declare that this is NOT a government app.
- [ ] **Financial features declaration** - Declare that this app does NOT provide financial services.
- [ ] **Health apps declaration** - Declare that this app does NOT provide health features.

---

## 7. Pricing and Distribution

- [ ] **Pricing** - Set the app as FREE. Note: Once an app is published as free, it cannot be changed to paid.
- [ ] **Countries and regions** - Select all countries/regions where you want the app to be available (or select "All countries").
- [ ] **Device compatibility** - Review the list of supported devices. The app requires Android 8.0+ (API 26) which covers the vast majority of active devices.

---

## 8. Release Tracks (Recommended Progression)

Follow this release progression to catch issues before reaching all users:

### 8.1 Internal Testing Track
- [ ] **Create an internal testing release** - Upload the AAB file.
- [ ] **Add internal testers** - Add up to 100 testers by email. Internal testers get access immediately without review.
- [ ] **Test thoroughly** - Verify all features work on multiple devices and Android versions.
- [ ] **Verify PIN authentication** works correctly with the PC server.
- [ ] **Verify all gesture types** - single tap, double tap, two-finger tap, three-finger tap, scroll, pinch, drag-and-drop, three-finger swipe.
- [ ] **Verify keyboard input** - QWERTY, function keys, numpad, and text input field.
- [ ] **Verify all shortcuts** execute correctly on the PC.

### 8.2 Closed Testing Track (Alpha/Beta)
- [ ] **Promote to closed testing** or create a new closed testing release.
- [ ] **Add closed testers** - Add testers via email lists or Google Groups. Up to 2,000 testers.
- [ ] **Collect feedback** - Use the feedback channel to gather bug reports and usability feedback.
- [ ] **Iterate and fix** any issues found during closed testing.

### 8.3 Open Testing Track (Beta)
- [ ] **Promote to open testing** - Anyone can join the test from the Play Store listing.
- [ ] **Monitor reviews and crash reports** in the Play Console.
- [ ] **Fix any remaining issues** before production release.

### 8.4 Production Release
- [ ] **Promote to production** or create a production release.
- [ ] **Choose rollout percentage** - Consider a staged rollout (e.g., 20% initially, then increase to 100%) to catch issues early.
- [ ] **Submit for review** - Google will review the app. Initial review typically takes 1-7 days for new developer accounts. Subsequent updates are usually faster.
- [ ] **Monitor post-launch** - Check the Play Console dashboard for:
  - ANR (Application Not Responding) reports.
  - Crash reports and stack traces.
  - User reviews and ratings.
  - Installation and uninstallation statistics.

---

## 9. Post-Launch

- [ ] **Respond to user reviews** - Engage with user feedback promptly and professionally.
- [ ] **Monitor Android vitals** - Keep ANR rate below 0.47% and crash rate below 1.09% to avoid Play Console warnings.
- [ ] **Plan updates** - Prepare for version 1.1.0 with improvements based on user feedback.
- [ ] **Update store listing** - Refresh screenshots and descriptions as the app evolves.
- [ ] **Keep dependencies updated** - Regularly update OkHttp, AndroidX libraries, and other dependencies for security and compatibility.
- [ ] **Update target SDK** - Google Play requires apps to target a recent API level. Update `targetSdk` in `build.gradle.kts` as new Android versions are released.

---

## Quick Reference: File Locations

| Item | Path |
|------|------|
| Release AAB | `android-app/app/build/outputs/bundle/release/app-release.aab` |
| Release APK | `android-app/app/build/outputs/apk/release/app-release.apk` |
| Keystore config | `android-app/keystore.properties` |
| Spanish listing | `docs/playstore/listing-es.md` |
| English listing | `docs/playstore/listing-en.md` |
| Privacy policy | `docs/playstore/privacy-policy.md` |
| This checklist | `docs/playstore/release-checklist.md` |
