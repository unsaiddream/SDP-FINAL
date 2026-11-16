from pathlib import Path
import zipfile

root = Path(__file__).resolve().parents[1]
output = root / 'target' / 'submission'
output.mkdir(parents=True, exist_ok=True)
jar = root / 'target' / 'event-studio.jar'
report = root / 'docs' / 'report.pdf'
if not jar.is_file() or not report.is_file():
    raise SystemExit('Build the application and PDF report before packaging.')

source_files = [root / 'pom.xml', root / 'README.md', root / '.gitignore']
for folder in ('src', 'docs', 'scripts'):
    source_files.extend(p for p in (root / folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts and p.name != '.DS_Store')
with zipfile.ZipFile(output / 'SDP-FINAL-source.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
    for path in sorted(source_files):
        archive.write(path, Path('SDP-FINAL') / path.relative_to(root))
with zipfile.ZipFile(output / 'Event-Studio-1.0.0.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
    archive.write(jar, 'Event-Studio/event-studio.jar')
    archive.write(root / 'README.md', 'Event-Studio/README.md')
    archive.writestr('Event-Studio/START.txt', 'Requires Java 17+ and a graphical desktop.\nOpen a terminal in this folder and run:\njava -jar event-studio.jar\nOptional isolated workspace:\njava -jar event-studio.jar /absolute/path/to/studio.json\nSample data is created on first launch.\n')
    for path in sorted((root / 'docs').rglob('*')):
        if path.is_file() and path.name != '.DS_Store':
            archive.write(path, Path('Event-Studio') / path.relative_to(root))
for path in sorted(output.glob('*.zip')):
    with zipfile.ZipFile(path) as archive:
        if archive.testzip() is not None:
            raise SystemExit('Archive verification failed: ' + str(path))
    print(f'{path.relative_to(root)} ({path.stat().st_size:,} bytes)')
