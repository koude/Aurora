# Aurora release checklist

## Automated before tagging

- Run `:app:testAlphaDebugUnitTest` and `:app:compileAlphaReleaseKotlin`.
- Confirm the version name and integer code both increase, and that the new tag points to the
  release commit.
- Check the tracked diff for accidental changes to `:core`, VPN service, signing files and
  unrelated assets.
- For a pre-release, confirm the workflow attaches one signed arm64-v8a APK. A formal release
  requires `docs/releases/<version>.md` and must attach five signed APKs: arm64-v8a,
  armeabi-v7a, x86, x86_64 and universal. Check the matching tag's release after GitHub Actions
  completes; a successful Git push alone does **not** prove publication.

## Manual smoke test on the GitHub-signed APK

- Launch and restart the app; confirm the last top-level page is restored.
- Switch among Home, Proxy, Connections and Settings; confirm only the intended page is visible.
- Add a URL and local-file profile; confirm progress is stable, success yields one profile, and
  failure leaves no partial profile. Update, activate and delete a profile.
- Connect/disconnect VPN and check real network traffic, mode switching and per-app proxy rules.
- Expand a proxy group, switch pages and return; check expansion behavior and latency display.
- Refresh Connections and close a filtered set; confirm unrelated connections remain.
- Check navigation and bottom-bar safe areas on at least one real device.

Do not install a locally signed APK over the user's real-phone GitHub build. Local emulator
testing is separate and must not be represented as real-device acceptance.
