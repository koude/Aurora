# Aurora project status

## v0.3.1

- Core loading fix: force native extraction + legacy JNI packaging.
- Runtime APK extraction fallback for `libclash.so` and `libmihomo-jni.so`.
- CI verifies both arm64 `.so` files exist inside the signed APK before upload.
- Logging from v0.3.0 retained for verification on-device.
