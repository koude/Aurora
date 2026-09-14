# Aurora CMFA Baseline

Aurora is a personal build based on MetaCubeX/ClashMetaForAndroid.

Baseline policy:
- Keep upstream CMFA VPN, service, network lifecycle, profile and mihomo integration intact.
- Android applicationId is supplied at build time as `com.koude.aurora`.
- Keep GPL-3.0 `LICENSE` and upstream `NOTICE`.
- Remove only nonessential externally exported automation/deep-link/secret-code entry points in this baseline.
- UI redesign is intentionally deferred until the CMFA baseline passes connection/background/network-switch tests.
