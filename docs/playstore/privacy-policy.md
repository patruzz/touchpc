# Privacy Policy for TouchPC - Remote Control

**Effective Date:** February 21, 2026
**Last Updated:** February 21, 2026
**App Name:** TouchPC - Remote Control
**Package Name:** com.touchpc.remotecontrol
**Developer Contact:** patriciomartinmendez@gmail.com

---

## Overview

TouchPC - Remote Control ("the App") is a utility application that allows users to control their PC's keyboard and mouse from an Android device over a local WiFi network. This Privacy Policy explains how the App handles user information and data.

We are committed to protecting your privacy. The App is designed to operate entirely within your local network and does not collect, transmit, or share any personal data with external parties.

---

## Data Collection

**The App does not collect any personal data.**

Specifically, the App:

- Does NOT collect any personally identifiable information (PII).
- Does NOT collect usage analytics or statistics.
- Does NOT use any third-party analytics services (such as Google Analytics, Firebase Analytics, or similar).
- Does NOT contain advertising or ad-tracking SDKs.
- Does NOT track user behavior, location, or device information beyond the local network connection.
- Does NOT access contacts, camera, microphone, files, or any other sensitive device resources.

---

## Data Stored Locally on Device

The App stores a minimal amount of data locally on your device using Android's DataStore Preferences. This data never leaves your device and is not transmitted to any server or third party. The locally stored data includes:

- **Touchpad sensitivity settings:** Your preferred cursor sensitivity, acceleration, and scroll speed values.
- **App preferences:** Tap-to-click toggle, natural scrolling preference, haptic feedback preference, and theme selection (light, dark, or system default).
- **Server connection history:** IP addresses and port numbers of previously connected PC servers (up to 10 entries), along with a display name and last connection timestamp. This is stored solely for user convenience to enable quick reconnection.

This data is stored only on your device and can be cleared at any time through the App's settings or by clearing the App's data through Android system settings.

---

## Network Communication

The App communicates exclusively over your local area network (LAN/WiFi). All communication between the App and the TouchPC server running on your PC occurs through WebSocket connections on your private local network.

Specifically:

- **All data transmission is local.** No data is sent to the internet, to remote servers, or to any third party.
- **Connection security.** The App uses a PIN-based authentication handshake to establish a connection with the PC server. This prevents unauthorized devices from controlling your PC.
- **No internet required.** The App does not require an internet connection to function. It only requires that both the Android device and the PC be connected to the same local WiFi network.
- **Data transmitted.** The only data sent over the local network consists of input control commands (mouse movements, clicks, key presses, scroll events, and keyboard shortcuts). No personal data, device identifiers, or user information is transmitted.

---

## Third-Party Services

The App does not integrate with or send data to any third-party services. There are:

- No third-party analytics SDKs.
- No advertising networks.
- No crash reporting services that transmit data externally.
- No social media integrations.
- No cloud storage or synchronization services.

---

## Children's Privacy

The App does not collect any data from any users, including children. The App is rated for all ages (Everyone) and is safe for use by users of any age group since it does not collect, store, or share any personal information beyond the local device preferences described above.

---

## Permissions

The App requests the following Android permissions:

- **INTERNET / ACCESS_NETWORK_STATE:** Required to establish a WebSocket connection to the TouchPC server on your local network. This permission is used exclusively for local network communication and not for accessing the internet.
- **FOREGROUND_SERVICE / FOREGROUND_SERVICE_CONNECTED_DEVICE:** Required to maintain a persistent connection to your PC server while the App runs in the background.
- **POST_NOTIFICATIONS:** Required on Android 13+ to display the foreground service notification that indicates an active connection.
- **VIBRATE:** Required to provide haptic feedback when interacting with the touchpad and keyboard.

---

## Data Security

Since the App operates exclusively within your local network:

- No data traverses the public internet.
- The PIN-based authentication prevents unauthorized connections.
- All locally stored preferences are handled through Android's standard DataStore mechanism, which stores data in the App's private internal storage directory, accessible only to the App itself.

---

## Changes to This Privacy Policy

We may update this Privacy Policy from time to time. Any changes will be reflected by updating the "Last Updated" date at the top of this document. We encourage users to review this page periodically for any changes.

---

## Your Rights

Since the App does not collect any personal data, there is no personal data to access, modify, or delete from our end. All data stored by the App resides locally on your device and is under your full control. You may clear all App data at any time through your Android device settings.

---

## Contact Us

If you have any questions or concerns about this Privacy Policy or the App's data practices, please contact us at:

**Email:** patriciomartinmendez@gmail.com

---

*This privacy policy applies to version 1.0.0 and subsequent versions of the TouchPC - Remote Control application (com.touchpc.remotecontrol) distributed through the Google Play Store.*
