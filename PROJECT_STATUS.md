# Aurora project status

## v0.2 implemented

- [x] App UI shell
- [x] Local YAML import
- [x] Android VPN permission flow
- [x] VpnService lifecycle
- [x] Quick Settings tile
- [x] Foreground notification + disconnect action
- [x] Real libmihomo Android bridge
- [x] Core load / bridge ABI validation
- [x] quickSetup(profile) before TUN start
- [x] TUN socket protection callback
- [x] Clean stop path (`stopTun` then close VPN fd)
- [x] arm64-v8a-only packaging

## next useful work

- [ ] Read proxy groups / nodes from the imported profile/core
- [ ] Real node switching via invokeAction
- [ ] Live delay test
- [ ] Live traffic counters
- [ ] Subscription URL import/update
- [ ] IPv6 path
- [ ] Persist UI settings with DataStore
- [ ] Release signing / GitHub Actions APK build

## v0.2.2

- Aligns Java source/target compatibility with Kotlin at Java 17.
- Includes `.github/workflows/build.yml` for GitHub Actions debug APK builds.
