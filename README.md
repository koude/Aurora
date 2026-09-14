# Aurora v0.2.7

Current app version: 0.2.7

Aurora is a minimal Android VPN client shell around libmihomo-android.

## v0.2.7 changes

- Added local YAML and http/https link import.
- Link import downloads to a temporary file, validates basic content, then atomically replaces `config.yaml`.
- Added explicit VPN permission, service-start and core-start feedback through Snackbar/status text.
- Updated core/status wording to reflect the real libmihomo integration.
- Kept GitHub Actions as artifact-only build; no automatic Release/tagging.
- Node list is still demonstration data and is not yet wired to the live mihomo controller.

## Build

GitHub Actions uses Java 17, Android SDK 35 and Gradle 8.9. The build task downloads the pinned libmihomo-android v0.3.1 AAR and packages the arm64-v8a native libraries into the APK.
