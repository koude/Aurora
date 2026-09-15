from pathlib import Path

root = Path.cwd()
wf = root / '.github' / 'workflows'
wf.mkdir(parents=True, exist_ok=True)

# Remove upstream workflows; keep only Aurora build workflow.
for p in wf.glob('*.yml'):
    if p.name != 'build-aurora.yml':
        p.unlink()
for p in wf.glob('*.yaml'):
    p.unlink()

src = Path(__file__).resolve().parent / '.github' / 'workflows' / 'build-aurora.yml'
dst = wf / 'build-aurora.yml'
dst.write_text(src.read_text())

print('Aurora CMFA build workflow updated.')
