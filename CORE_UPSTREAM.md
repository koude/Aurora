# Core upstream

Pinned Android wrapper: https://github.com/oviron/libmihomo-android
Pinned wrapper version: v0.3.1
Bundled mihomo version reported by upstream: v1.19.28
Bridge ABI: 3
Upstream mihomo: https://github.com/MetaCubeX/mihomo

Aurora intentionally does not rename the native libraries inside the AAR. `Clash.load(...)` expects the upstream native loading layout. Renaming the AAR file itself is harmless; renaming the `.so` files requires rebuilding/patching the JNI facade and offers little practical privacy benefit.
