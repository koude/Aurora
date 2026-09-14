# Aurora v0.2.9

Current app version: 0.2.9

Aurora is a minimal Android VPN client shell around libmihomo-android.

## v0.2.9 changes

- Application ID remains `com.koude.aurora`.
- Keeps the Clash Meta-aligned VPN consent flow from v0.2.7.
- Adds a temporary manual GitHub Actions workflow: `Generate Aurora Keystore`.
- The workflow generates a fixed `aurora-release.jks` using repository secrets and uploads it as an artifact.
- The keystore generator is temporary and should be removed after the key is downloaded and backed up.
- Normal APK build remains artifact-only; no automatic GitHub Release/tagging.

## Before running Generate Aurora Keystore

Create these repository Actions secrets:

- `AURORA_STORE_PASSWORD`
- `AURORA_KEY_PASSWORD`

The key alias is fixed as `aurora`.

Then open GitHub Actions -> Generate Aurora Keystore -> Run workflow. Download the `aurora-release-keystore` artifact and keep `aurora-release.jks` backed up safely.

## Build

GitHub Actions uses Java 17, Android SDK 35 and Gradle 8.9. The normal build downloads the pinned libmihomo-android v0.3.1 AAR and packages the arm64-v8a native libraries into the APK.
