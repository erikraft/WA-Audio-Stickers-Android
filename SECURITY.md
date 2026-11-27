# Security Policy

## Supported versions
Security fixes target the latest code on the default branch and active release line.

## Reporting
Do not publish exploit details, personal media, tokens or private files in a public issue. Use GitHub private vulnerability reporting or a Security Advisory when available.

## Security boundaries
- The Android UI, media selection, preview and export are implemented natively in Kotlin; the application does not embed a WebView or JavaScript bridge.
- The app uses Android Storage Access Framework document pickers and retains read access only to user-selected content URIs.
- Media3 Transformer processes selected media locally and writes generated MP4 files only under the app's private cache directory.
- A non-exported FileProvider grants read-only access to the specific generated file shared with WhatsApp or WhatsApp Business.
- Temporary exports are cleaned by age when the app starts.
- The app does not request broad storage permissions, declare the INTERNET permission, or expose an app backend/network endpoint.
