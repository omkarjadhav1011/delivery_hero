import { readdirSync, readFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { describe, expect, it } from "vitest";

// The theme in app/globals.css against document 12, sections 5.2 and 5.3 (DEC-166, DEC-167)
const frontendRoot = resolve(__dirname, "../..");
const globalsCss = readFileSync(join(frontendRoot, "app/globals.css"), "utf8");
const document12 = readFileSync(join(frontendRoot, "../docs/12-ui-ux-wireframes.md"), "utf8");

function section(markdown: string, heading: string): string {
  const start = markdown.indexOf(`### ${heading}`);
  expect(start, `document 12 has "${heading}"`).toBeGreaterThanOrEqual(0);
  const end = markdown.indexOf("\n### ", start + 1);
  return markdown.slice(start, end === -1 ? undefined : end);
}

function themeValue(name: string): string | undefined {
  const match = new RegExp(`${name}:\\s*([^;]+);`).exec(globalsCss);
  return match?.[1]?.trim();
}

function sourceFiles(dir: string): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) return sourceFiles(path);
    return /\.tsx?$/.test(entry.name) && !entry.name.includes(".test.") ? [path] : [];
  });
}

describe("theme", () => {
  it("AC-EN08-01 color tokens equal document 12 section 5.2", () => {
    const table = section(document12, "5.2 Color tokens");
    const rows = [...table.matchAll(/^\| `--([a-z0-9-]+)` \| `(#[0-9A-Fa-f]{6})` \|/gm)];
    expect(rows).toHaveLength(14);
    for (const [, token, value] of rows) {
      expect(themeValue(`--color-${token}`), `--color-${token}`).toBe(value?.toLowerCase());
    }
    const defined = [...globalsCss.matchAll(/--color-([a-z0-9-]+):/g)].map((match) => match[1]);
    expect(defined.sort()).toEqual(rows.map((row) => row[1]).sort());
  });

  it("AC-EN08-01 font stacks equal document 12 section 5.3", () => {
    const table = section(document12, "5.3 Typography");
    const stack = (role: string) =>
      new RegExp(`^\\| ${role} \\| [^(]*\\(\`([^\`]+)\`\\)`, "m").exec(table)?.[1];
    expect(themeValue("--font-sans")).toBe(stack("Text"));
    expect(themeValue("--font-mono")).toBe(stack("Code"));
    expect(themeValue("--font-display")).toMatch(/^var\(--font-press-start-2p\),/);
  });

  it("AC-EN08-01 the pixel font is never used below 1 rem", () => {
    // Tailwind's text-xs and text-sm are 0.75 and 0.875 rem; arbitrary sizes must be 1 rem (16 px) or more
    const small = /\btext-(xs|sm)\b|\btext-\[(0?\.\d+rem|(1[0-5]|\d)px)\]/;
    const offenders: string[] = [];
    for (const file of [
      ...sourceFiles(join(frontendRoot, "app")),
      ...sourceFiles(join(frontendRoot, "src")),
    ]) {
      for (const [className] of readFileSync(file, "utf8").matchAll(
        /"[^"\n]*\bfont-display\b[^"\n]*"/g,
      )) {
        if (small.test(className)) offenders.push(`${file}: ${className}`);
      }
    }
    expect(offenders).toEqual([]);
  });

  it("AC-EN08-01 a 3 px focus ring and reduced motion are set globally", () => {
    expect(globalsCss).toMatch(/:focus-visible\s*\{[^}]*outline:\s*3px solid var\(--color-focus\)/);
    expect(globalsCss).toMatch(/@media \(prefers-reduced-motion: reduce\)/);
  });
});
