Aurora CMFA baseline patch

Apply this only after main has been reset to MetaCubeX/ClashMetaForAndroid main and submodules initialized.
Run:
  python3 apply_aurora_baseline.py
Then copy .github/workflows/build-aurora.yml into the repository if this package is not already overlaid.

Required GitHub Actions secrets:
  AURORA_KEYSTORE_BASE64
  AURORA_STORE_PASSWORD
  AURORA_KEY_PASSWORD
The keystore alias is fixed to: aurora
