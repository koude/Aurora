# Aurora 1.0 regression checks

Automated checks:

- `app:testAlphaDebugUnitTest` covers route persistence, profile import, connection filtering, and other presentation/data rules.
- `app:connectedAlphaDebugAndroidTest` runs Compose smoke tests for the four top-level pages, profile-add sources, proxy refresh scope, and filtered connection closing. Run it on an Android emulator; Gradle installs and removes its test packages there.

Manual emulator smoke test with `fixtures/direct-only.yaml`:

1. In Settings → Profiles, import the file using Local file, then activate it.
2. Start Aurora from Home and accept the emulator VPN prompt.
3. In Proxy, confirm `Aurora 测试出口 → 本地直连` appears; expand it, switch tabs, and confirm the group collapses on return.
4. In Connections, check search and filtered close. Check route preview and website latency from Home when the emulator has network access.
5. Force-stop and reopen while Settings is selected; Settings should be restored.
6. Review Home, Proxy, Connections, Settings, and Profiles in light/dark mode and at a 1.3 font scale. Check that no content overlaps the gesture area.

The fixture contains no credentials and uses a local direct proxy. Do not use it to judge remote-proxy speed or Geo-download reliability.

For a public release, verify that the GitHub Actions arm64-v8a APK is signed with the established Aurora signing key. Never install a local debug build over a user's signed phone installation.
