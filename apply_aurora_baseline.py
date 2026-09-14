#!/usr/bin/env python3
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path.cwd()

def replace(path, old, new):
    p = ROOT / path
    if not p.exists():
        print(f"skip missing: {path}")
        return
    s = p.read_text(encoding='utf-8')
    if old not in s:
        print(f"pattern not found: {path}: {old}")
        return
    p.write_text(s.replace(old, new), encoding='utf-8')
    print(f"updated: {path}")

# Project / artifact branding. Keep upstream Kotlin namespaces intact to minimize divergence.
replace('settings.gradle.kts', 'rootProject.name = "ClashMetaForAndroid"', 'rootProject.name = "Aurora"')
replace('build.gradle.kts', 'setProperty("archivesBaseName", "cmfa-$versionName")', 'setProperty("archivesBaseName", "aurora-$versionName")')

# Rename user-visible launch/application labels without touching protocol/core strings.
name_keys = {
    'application_name_alpha', 'application_name_meta',
    'launch_name_alpha', 'launch_name_meta'
}
for p in ROOT.rglob('strings.xml'):
    try:
        text = p.read_text(encoding='utf-8')
    except Exception:
        continue
    orig = text
    for key in name_keys:
        text = re.sub(
            rf'(<string\s+name=["\']{re.escape(key)}["\'][^>]*>).*?(</string>)',
            rf'\1Aurora\2', text, flags=re.S
        )
    if text != orig:
        p.write_text(text, encoding='utf-8')
        print(f"rebranded labels: {p.relative_to(ROOT)}")

# Reduce nonessential public surfaces. VPN/core/service behavior stays upstream.
manifest = ROOT / 'app/src/main/AndroidManifest.xml'
if manifest.exists():
    ET.register_namespace('android', 'http://schemas.android.com/apk/res/android')
    ET.register_namespace('tools', 'http://schemas.android.com/tools')
    tree = ET.parse(manifest)
    root = tree.getroot()
    android = '{http://schemas.android.com/apk/res/android}'

    # Protected cross-profile permission is not useful for this personal baseline.
    for perm in list(root.findall('uses-permission')):
        if perm.get(android + 'name') == 'android.permission.INTERACT_ACROSS_USERS':
            root.remove(perm)
            print('removed INTERACT_ACROSS_USERS permission')

    app = root.find('application')
    if app is not None:
        # Disable exported automation / clash:// entry points; keep activity class available internally.
        for act in app.findall('activity'):
            if act.get(android + 'name') == '.ExternalControlActivity':
                act.set(android + 'exported', 'false')
                for f in list(act.findall('intent-filter')):
                    act.remove(f)
                print('disabled ExternalControlActivity public intent filters')

        # Remove secret dialer code receiver entirely.
        for recv in list(app.findall('receiver')):
            if recv.get(android + 'name') == '.DialerReceiver':
                app.remove(recv)
                print('removed DialerReceiver secret-code entry point')

    tree.write(manifest, encoding='utf-8', xml_declaration=True)
    print('updated AndroidManifest.xml')

# Add baseline note while retaining upstream GPL/NOTICE untouched.
notes = ROOT / 'AURORA_BASELINE.md'
notes.write_text('''# Aurora CMFA Baseline\n\nAurora is a personal build based on MetaCubeX/ClashMetaForAndroid.\n\nBaseline policy:\n- Keep upstream CMFA VPN, service, network lifecycle, profile and mihomo integration intact.\n- Android applicationId is supplied at build time as `com.koude.aurora`.\n- Keep GPL-3.0 `LICENSE` and upstream `NOTICE`.\n- Remove only nonessential externally exported automation/deep-link/secret-code entry points in this baseline.\n- UI redesign is intentionally deferred until the CMFA baseline passes connection/background/network-switch tests.\n''', encoding='utf-8')
print('wrote AURORA_BASELINE.md')
