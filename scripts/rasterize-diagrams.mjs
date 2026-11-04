// Renderiza os SVGs locais no Chromium, preservando rótulos HTML/CSS do Mermaid.
import { chromium } from 'playwright';
import { readdir } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const options = { headless: true };
if (process.argv[2]) options.executablePath = process.argv[2];
const browser = await chromium.launch(options);
try {
  const page = await browser.newPage({ deviceScaleFactor: 3 });
  const diagrams = ['docs/mer.svg', ...(await readdir(join(root, 'docs/diagrams')))
    .filter((name) => name.endsWith('.svg')).map((name) => `docs/diagrams/${name}`)];
  for (const relative of diagrams) {
    await page.goto(pathToFileURL(join(root, relative)).href);
    await page.evaluate(() => document.fonts.ready);
    const svg = page.locator('svg');
    const native = await svg.evaluate((element) => {
      const view = element.viewBox.baseVal;
      element.style.width = `${view.width}px`;
      element.style.height = `${view.height}px`;
      element.style.maxWidth = 'none';
      return { width: Math.ceil(view.width + 16), height: Math.ceil(view.height + 16) };
    });
    await page.setViewportSize(native);
    const box = await svg.boundingBox();
    if (!box || box.width < 10 || box.height < 10) throw new Error(`SVG vazio: ${relative}`);
    await svg.screenshot({ path: join(root, relative.replace(/\.svg$/, '.png')) });
  }
  process.stdout.write(`${diagrams.length} SVGs renderizados em PNG 3x.\n`);
} finally {
  await browser.close();
}
