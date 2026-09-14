# Core upstream

Aurora v0.2.4 uses libmihomo-android v0.3.1 as its Android JNI bridge to mihomo.

The AAR is downloaded at build time and embedded in the resulting APK. Aurora does not download the executable core after installation.

The JNI/native library names are intentionally left as provided by upstream to avoid breaking the library loading chain.
