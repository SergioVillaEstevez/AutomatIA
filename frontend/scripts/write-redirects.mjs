import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const apiOrigin = (process.env.NEIVER_API_ORIGIN || '').replace(/\/$/, '');
if (!apiOrigin) {
  console.error('Falta NEIVER_API_ORIGIN (URL del API en Render, sin barra final).');
  process.exit(1);
}

const publishDir = join(dirname(fileURLToPath(import.meta.url)), '..', 'dist', 'frontend', 'browser');
mkdirSync(publishDir, { recursive: true });

const redirects = `# Generado en el build. No editar a mano.
/api/*  ${apiOrigin}/api/:splat  200!
/*      /index.html              200
`;

writeFileSync(join(publishDir, '_redirects'), redirects);
console.log(`Proxy /api -> ${apiOrigin}`);
