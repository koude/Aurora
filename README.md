# Aurora v0.3.0

Current app version: 0.3.0

Aurora is a minimal Android VPN client shell around libmihomo-android.

## v0.3.0 changes

- Adds an in-app diagnostic log page under Settings -> Logs.
- Logs VPN consent, service lifecycle, config import, native core loading, bridge ABI, TUN creation, quickSetup/startTUN and full exception stack traces.
- Logs are local only, rotated at about 2 MB, and can be copied, exported or cleared by the user.
- Core load failures now include error code `CORE-LOAD-001` and direct the user to the log page.
- Application ID remains `com.koude.aurora`.
- Keeps libmihomo-android v0.3.1 and the Clash Meta-aligned VPN consent flow.
- Removes the temporary keystore-generation workflow after the signing key has been generated.
- Normal GitHub Actions build now produces a signed Release APK using the fixed Aurora keystore stored in repository secrets.
- No automatic GitHub Release/tagging.

## Required GitHub Actions secrets

The build requires:

- `AURORA_KEYSTORE_BASE64`
- `AURORA_STORE_PASSWORD`
- `AURORA_KEY_PASSWORD`

The key alias is fixed as `aurora`.

## Build

GitHub Actions uses Java 17, Android SDK 35 and Gradle 8.9. The build downloads the pinned libmihomo-android v0.3.1 AAR, packages arm64-v8a native libraries, and signs `app-release.apk` with the fixed Aurora key.
