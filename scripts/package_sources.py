"""Empacota somente fontes e documentação, incluindo o PDF conferido."""
from pathlib import Path
from hashlib import sha256
from zipfile import ZipFile, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parent.parent
PDF = ROOT / 'output/pdf/Alpha4_Sprint4_Java.pdf'
if not PDF.is_file():
    raise SystemExit('Gere e revise o PDF antes de empacotar.')

allowed_files = [
    'pom.xml', 'README.md', 'Dockerfile', '.dockerignore', '.gitignore',
    '.gitattributes', '.env.example', 'mvnw', 'mvnw.cmd',
]
allowed_dirs = ['src', '.mvn', 'docs', 'scripts', 'output/pdf']
files = [ROOT / name for name in allowed_files]
for directory in allowed_dirs:
    files.extend(path for path in (ROOT / directory).rglob('*')
                 if path.is_file() and not {'__pycache__', 'node_modules'} & set(path.parts) and path.suffix != '.pyc')
files = sorted(set(files))
if not all(path.is_file() for path in files):
    raise SystemExit('Arquivo essencial ausente na lista de empacotamento.')

destination = ROOT / 'entregas'
destination.mkdir(exist_ok=True)
archive = destination / 'Alpha4_Sprint4_Java_fontes.zip'
with ZipFile(archive, 'w', ZIP_DEFLATED, compresslevel=9) as package:
    for path in files:
        package.write(path, Path('challengesprints-java') / path.relative_to(ROOT))
with ZipFile(archive) as package:
    if package.testzip() is not None:
        raise SystemExit('Falha de integridade no ZIP.')
    entries = package.namelist()
    if not any(name.endswith('output/pdf/Alpha4_Sprint4_Java.pdf') for name in entries):
        raise SystemExit('PDF ausente no ZIP.')
    if any({'data', 'target', '.git', 'tmp', '__pycache__', 'node_modules'} & set(Path(name).parts) for name in entries):
        raise SystemExit('O ZIP contém um diretório excluído.')
digest = sha256(archive.read_bytes()).hexdigest()
archive.with_suffix('.zip.sha256').write_text(f'{digest}  {archive.name}\n', encoding='ascii')
print(f'ZIP íntegro: {len(entries)} arquivos, {archive.stat().st_size:,} bytes.')
print(f'SHA-256: {digest}')
