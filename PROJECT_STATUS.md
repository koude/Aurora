# Aurora project status

## v0.3.0

Implemented:
- Jetpack Compose home / nodes / settings UI.
- Android VpnService and Quick Settings tile.
- libmihomo-android v0.3.1 core bridge.
- Local YAML configuration import.
- http/https configuration URL import.
- Visible connection progress and failure feedback.
- Local rotating diagnostic log with copy/export/clear controls.
- Core/native load diagnostics including full stack traces.
- arm64-v8a APK target.
- Application ID / namespace: `com.koude.aurora`.
- VPN consent flow aligned with Clash Meta for Android.
- Fixed release signing in GitHub Actions using repository secrets.

Not yet implemented:
- Live proxy group / node data from mihomo.
- Real node switching.
- Latency testing.
- Boot auto-connect.
- Automatic GitHub Release publishing.
