# Aurora release checklist

## Automated before tagging

- Run `:app:testAlphaDebugUnitTest` and `:app:compileAlphaReleaseKotlin`.
- Confirm the version name and integer code both increase, and that the new tag points to the
  release commit.
- Check the tracked diff for accidental changes to `:core`, VPN service, signing files and
  unrelated assets.
- The GitHub workflow must produce one signed arm64-v8a APK and attach it to the matching tag's
  release. A successful Git push or local Kotlin compilation alone does **not** prove this step.

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

## Current host limitation

On the present Windows machine, full local APK assembly is blocked by Windows Application
Control rejecting the repo-local Go `cgo.exe`; offline Gradle also lacks cached
`com.android.tools.lint:lint-gradle:31.8.0`. Kotlin compilation and unit tests can run. The
GitHub Actions run/release must be checked separately before calling a tagged version released.
