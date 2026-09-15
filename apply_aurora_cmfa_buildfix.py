#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path.cwd()
WF_DIR = ROOT / '.github' / 'workflows'
KEEP = {'build-aurora.yml'}

# Keep only Aurora's own workflow. This intentionally removes CMFA upstream
# debug / prerelease / release / dependency-update workflows from this personal fork.
if WF_DIR.exists():
    for p in WF_DIR.iterdir():
        if p.is_file() and p.suffix.lower() in {'.yml', '.yaml'} and p.name not in KEEP:
            p.unlink()
            print(f'removed upstream workflow: {p.relative_to(ROOT)}')

cmake = ROOT / 'core' / 'src' / 'main' / 'cpp' / 'CMakeLists.txt'
if not cmake.exists():
    raise SystemExit(f'missing CMFA CMake file: {cmake}')

text = cmake.read_text(encoding='utf-8')
orig = text

# CMFA derives a branch name from the mihomo submodule. In CI the submodule can
# be detached, and the old block assumes a second list element always exists.
# Replace it with a guarded fallback and quote regex input so an empty value
# never changes the CMake argument count.
pattern = re.compile(
    r'string\(REPLACE\s+"\\n"\s+";"\s+CURRENT_BRANCH\s+"\$\{CURRENT_BRANCH\}"\)\s*\n'
    r'\s*list\(GET\s+CURRENT_BRANCH\s+1\s+CURRENT_BRANCH\)\s*\n'
    r'\s*string\(REGEX\s+REPLACE\s+"origin/"\s+""\s+CURRENT_BRANCH\s+"?\$\{CURRENT_BRANCH\}"?\)\s*\n'
    r'\s*string\(REGEX\s+REPLACE\s+"\[\\n\\t\\r\]"\s+""\s+CURRENT_BRANCH\s+"?\$\{CURRENT_BRANCH\}"?\)',
    re.M,
)
replacement = '''string(REPLACE "\\n" ";" CURRENT_BRANCH "${CURRENT_BRANCH}")
list(LENGTH CURRENT_BRANCH CURRENT_BRANCH_COUNT)
if(CURRENT_BRANCH_COUNT GREATER 1)
    list(GET CURRENT_BRANCH 1 CURRENT_BRANCH)
else()
    set(CURRENT_BRANCH "detached")
endif()
string(REGEX REPLACE "origin/" "" CURRENT_BRANCH "${CURRENT_BRANCH}")
string(REGEX REPLACE "[\\n\\t\\r]" "" CURRENT_BRANCH "${CURRENT_BRANCH}")'''
text, n = pattern.subn(replacement, text, count=1)

if n == 0:
    # Fallback for slightly different upstream formatting: at minimum quote the
    # regex inputs. Full-history checkout normally supplies the branch; quoting
    # also prevents the original six-argument CMake failure.
    text = text.replace(
        'string(REGEX REPLACE "origin/" "" CURRENT_BRANCH ${CURRENT_BRANCH})',
        'string(REGEX REPLACE "origin/" "" CURRENT_BRANCH "${CURRENT_BRANCH}")'
    )
    text = text.replace(
        'string(REGEX REPLACE "[\\n\\t\\r]" "" CURRENT_BRANCH ${CURRENT_BRANCH})',
        'string(REGEX REPLACE "[\\n\\t\\r]" "" CURRENT_BRANCH "${CURRENT_BRANCH}")'
    )
    print('warning: full CURRENT_BRANCH block not matched; applied safe quoting fallback')
else:
    print('patched CMFA CMake detached-submodule branch handling')

if text != orig:
    cmake.write_text(text, encoding='utf-8')
else:
    print('CMake already patched or upstream layout changed; no CMake text changed')

note = ROOT / 'AURORA_BUILD_NOTES.md'
note.write_text('''# Aurora CMFA build notes\n\nThis personal CMFA-based branch intentionally keeps only `.github/workflows/build-aurora.yml`.\n\nCI differences from upstream:\n- Full git history checkout (`fetch-depth: 0`).\n- Recursive submodule branch/tag fetch before CMake configuration.\n- CMake branch-name handling tolerates a detached mihomo submodule.\n- Aurora fixed signing secrets and `com.koude.aurora` application id remain unchanged.\n''', encoding='utf-8')
print('wrote AURORA_BUILD_NOTES.md')
