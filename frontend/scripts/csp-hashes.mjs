// Post-build step (LLD section 6.6, DEC-135): hashes every inline <script> without a src in the exported HTML and
// writes nginx/csp.conf with the full Content-Security-Policy header. Nginx includes it from
// /etc/nginx/snippets/csp.conf; deploy/nginx/snippets/csp.conf.example shows its shape.
import { createHash } from "node:crypto";
import { mkdir, readdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const exportDir = path.join(root, "out");
const target = path.join(root, "nginx", "csp.conf");

async function htmlFiles(dir) {
  const entries = await readdir(dir, { withFileTypes: true });
  const nested = await Promise.all(
    entries.map((entry) => {
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) return htmlFiles(full);
      return entry.name.endsWith(".html") ? [full] : [];
    }),
  );
  return nested.flat();
}

const SCRIPT = /<script\b([^>]*)>([\s\S]*?)<\/script\s*>/gi;
const HAS_SRC = /(?:^|\s)src\s*=/i;

function inlineScriptHashes(html) {
  const hashes = [];
  for (const [, attributes, content] of html.matchAll(SCRIPT)) {
    if (HAS_SRC.test(attributes)) continue;
    hashes.push(`'sha256-${createHash("sha256").update(content, "utf8").digest("base64")}'`);
  }
  return hashes;
}

const files = await htmlFiles(exportDir).catch(() => {
  throw new Error(`No static export in ${exportDir}: run "next build" first`);
});
if (files.length === 0) {
  throw new Error(`No HTML files in ${exportDir}`);
}

const hashes = new Set();
for (const file of files) {
  for (const hash of inlineScriptHashes(await readFile(file, "utf8"))) hashes.add(hash);
}

const policy = [
  ["script-src", "'self'", ...[...hashes].sort()],
  ["style-src", "'self'", "'unsafe-inline'"],
  ["connect-src", "'self'"],
  ["img-src", "'self'", "data:"],
  ["font-src", "'self'"],
  ["object-src", "'none'"],
  ["base-uri", "'self'"],
  ["frame-ancestors", "'none'"],
]
  .map((directive) => directive.join(" "))
  .join("; ");

await mkdir(path.dirname(target), { recursive: true });
await writeFile(
  target,
  "# Written by frontend/scripts/csp-hashes.mjs after every build (LLD section 6.6); don't edit it by hand\n" +
    `add_header Content-Security-Policy "${policy}" always;\n`,
);
console.log(
  `csp-hashes: ${hashes.size} inline script hashes from ${files.length} pages written to ${path.relative(root, target)}`,
);
