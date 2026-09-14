# Aurora project status

## v0.2.8

Implemented:
- Jetpack Compose home / nodes / settings UI.
- Android VpnService and Quick Settings tile.
- libmihomo-android v0.3.1 core bridge.
- Local YAML configuration import.
- http/https configuration URL import.
- Visible connection progress and failure feedback.
- arm64-v8a APK target.
- GitHub Actions debug APK artifact build.
- Application ID / namespace: `com.koude.aurora`.
- VPN consent flow aligned with Clash Meta for Android.
- Temporary manual workflow to generate a fixed JKS signing key.

Not yet implemented:
- Wiring the generated JKS into the normal APK signing workflow.
- Live proxy group / node data from mihomo.
- Real node switching.
- Latency testing.
- Boot auto-connect.
- Automatic GitHub Release publishing.

Next signing step:
1. Run `Generate Aurora Keystore` once.
2. Download and securely back up `aurora-release.jks`.
3. Convert/store the JKS in GitHub Actions secrets.
4. Update normal build to sign every APK with the same key.
5. Remove the temporary keystore-generation workflow.
