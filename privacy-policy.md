# Privacy Policy — Open Plaato Keg

**Last updated: March 2026**

## Overview

Open Plaato Keg ("the app") is an open-source Android application that connects to a self-hosted Open Plaato Keg server running on your local network. This privacy policy explains what data the app handles and how.

## Data Collection

**The app does not collect, store, or transmit any personal data.**

Specifically:
- No analytics or crash reporting services are used
- No advertising SDKs are included
- No data is sent to any third-party service by the app itself
- No account registration or login is required

## Local Network Communication

The app communicates exclusively with a server address entered by the user (e.g. `http://192.168.1.10:4000`). All communication stays within the user's local network. The app does not contact any external servers operated by the developer.

## Optional Third-Party Integrations

The app allows configuration of optional integrations (Brewfather, Grainfather) that are managed entirely on the self-hosted server. If these integrations are enabled, data is sent from **your server** to those services according to their own privacy policies. The app itself does not communicate directly with Brewfather or Grainfather.

## Data Storage

The app stores only one piece of data locally on the device: the server URL you enter in Settings. This is stored using Android's DataStore and never leaves the device.

## Permissions

The app requests only the `INTERNET` permission, which is required to communicate with your local server over Wi-Fi.

## Open Source

The full source code for this app is available at:
https://github.com/DarkJaeger/open-plaato-keg-android

## Contact

For questions about this privacy policy, please open an issue at the repository above.
