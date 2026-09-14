# Aurora v0.3.2

Current app version: 0.3.2

Aurora is a minimal Android VPN client shell around libmihomo-android.

## v0.3.2 changes

- Fixed mihomo native library loading on devices where the installed native library directory is empty.
- Forces Android to extract packaged native libraries and enables legacy JNI packaging for the release APK.
- Adds a runtime fallback that extracts `libclash.so` and `libmihomo-jni.so` directly from the APK into Aurora private storage when needed.
- GitHub Actions now verifies both arm64 native libraries are present in the final APK and fails the build if either is missing.
- Keystore restore is tolerant of whitespace/newlines in the Base64 secret.

## Required GitHub Actions secrets

The build requires:

- `AURORA_KEYSTORE_BASE64`
- `AURORA_STORE_PASSWORD`
- `AURORA_KEY_PASSWORD`

The key alias is fixed as `aurora`.

## Build

GitHub Actions uses Java 17, Android SDK 35 and Gradle 8.9. The build downloads the pinned libmihomo-android v0.3.2 AAR, packages arm64-v8a native libraries, and signs `app-release.apk` with the fixed Aurora key.
