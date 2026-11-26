# Security Policy

## Supported versions
Security fixes target the latest code on the default branch and active release line.

## Reporting
Do not publish exploit details, personal media, tokens or private files in a public issue. Use GitHub private vulnerability reporting or a Security Advisory when available.

## Security boundaries
- WebView content is bundled and served by AndroidX WebViewAssetLoader from the appassets.androidplatform.net origin.
- Navigation and subresource requests are restricted to bundled assets; GitHub links open outside the WebView.
- The native JavaScript bridge validates MIME, filename and Base64 length and writes only under private cache.
- Shared files use a non-exported FileProvider, read-only URI grants and a single cache subdirectory.
- The app uses Android Storage Access Framework and does not request broad storage permissions.
- No backend, account system, payments, database, webhook, AI API or app-level network endpoint exists.
