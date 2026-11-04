"""Renderiza todas as páginas para revisão visual, sem alterar o PDF."""
from pathlib import Path
import os
import sys
if os.environ.get('ALPHA4_PDF_TOOLS'):
    sys.path.insert(0, os.environ['ALPHA4_PDF_TOOLS'])
import pypdfium2 as pdfium

root = Path(__file__).resolve().parent.parent
pdf_path = root / 'output/pdf/Alpha4_Sprint4_Java.pdf'
target = root / 'tmp/pdfs/review'
target.mkdir(parents=True, exist_ok=True)
pdf = pdfium.PdfDocument(str(pdf_path))
for number, page in enumerate(pdf, 1):
    image = page.render(scale=1.25).to_pil()
    image.save(target / f'pagina-{number:02}.png')
    page.close()
pdf.close()
print(f'{number} páginas renderizadas em tmp/pdfs/review.')
