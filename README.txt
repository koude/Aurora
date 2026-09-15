Aurora CMFA baseline build patch v2

Use on the current Aurora main branch after the first CMFA baseline patch has already been applied.

Changes:
- Keep only .github/workflows/build-aurora.yml
- GitHub Actions checkout fetch-depth = 0
- Fetch all submodule branches/tags before build
- Make CMFA CMake branch-name detection safe for detached mihomo submodules
- No VPN/Core runtime behavior changes

Apply:
  copy this patch over the repository
  python3 apply_aurora_cmfa_buildfix.py
  git add .
  git commit -m "Fix Aurora CMFA build workflow"
  git push
