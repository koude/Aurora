# Aurora v0.2.2

Current app version: 0.2.2

Aurora is a small Android VPN client shell with a Compose UI and a real mihomo core bridge.

## What v0.2 contains

- Compose home / nodes / settings UI
- YAML profile import to app-private storage
- Android `VpnService`
- Quick Settings tile
- Foreground connection notification with a disconnect action
- Real mihomo bridge via `libmihomo-android` v0.3.1
- arm64-v8a packaging only (intended for current Android phones)

## Core

Aurora pins `oviron/libmihomo-android` **v0.3.1**, which bundles mihomo **v1.19.28** and bridge ABI 3.
The AAR is downloaded from its GitHub Release during the Gradle build and stored under the build directory with an Aurora-local filename. The native library names inside the AAR are intentionally left unchanged because the JNI facade loads those exact names.

The core is isolated behind `core/CoreBridge.kt`, so a future core upgrade should not require rewriting the UI or service layer.

## Open / build

1. Open this folder in a recent Android Studio.
2. Let Gradle sync. Internet access to GitHub is required the first time so the pinned AAR can be downloaded.
3. Build `app` for an arm64-v8a device.
4. Import a valid mihomo / Clash-Meta YAML profile.
5. Tap Connect and grant Android's VPN permission.

If GitHub is blocked on the build machine, manually download:

`libmihomo-android-v0.3.1.aar`

from the v0.3.1 release and adjust `app/build.gradle.kts` to point to the local file.

## Important current limits

- Node cards in the UI are still presentation/demo data; live proxy-group enumeration and switching are a later step.
- The VPN path is IPv4-only in v0.2.
- Process-name resolution returns unknown (`""`).
- No subscription manager yet; profiles are imported manually.
- No core auto-update; the version is deliberately pinned for reproducible builds.

## License note

mihomo and libmihomo-android are GPL-3.0. An APK that distributes the linked core needs to comply with the applicable GPL-3.0 obligations. See the upstream repositories for their license text and source.
