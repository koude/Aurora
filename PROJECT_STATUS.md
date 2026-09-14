# Aurora project status

## v0.2.6

Implemented:
- Jetpack Compose home / nodes / settings UI.
- Android VpnService and Quick Settings tile.
- libmihomo-android v0.3.1 core bridge.
- Local YAML configuration import.
- http/https configuration URL import.
- Visible connection progress and failure feedback.
- arm64-v8a APK target.
- GitHub Actions debug APK artifact build.

Not yet implemented:
- Live proxy group / node data from mihomo.
- Real node switching.
- Latency testing.
- Boot auto-connect.
- Automatic GitHub Release publishing.


## v0.2.6
- VPN Service manifest exported=true for OEM/system discovery.
- Added VPN authorization diagnostics (prepare intent action/component/resolver/result).
- Falls back to system VPN settings if no confirmation Activity can resolve the prepare Intent.
