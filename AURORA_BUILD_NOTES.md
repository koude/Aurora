# Aurora CMFA build notes

This personal CMFA-based branch intentionally keeps only `.github/workflows/build-aurora.yml`.

CI differences from upstream:
- Full git history checkout (`fetch-depth: 0`).
- Recursive submodule branch/tag fetch before CMake configuration.
- CMake branch-name handling tolerates a detached mihomo submodule.
- Aurora fixed signing secrets and `com.koude.aurora` application id remain unchanged.
