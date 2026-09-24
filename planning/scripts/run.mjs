// Runs one planning script with whichever Python 3 this machine has (py -3, python or python3),
// so skills can use one command on Windows, macOS and Linux:
//   node planning/scripts/run.mjs status --check
// The first argument is the script name without .py; the rest pass through. Exit codes pass through.
import { spawnSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const [script, ...args] = process.argv.slice(2);

if (!script || !/^[a-z_]+$/.test(script)) {
  process.stderr.write("Usage: node planning/scripts/run.mjs <script> [args]   (scripts: ids, docs_manifest, section, trace, validate, status, next, journal, state)\n");
  process.exit(2);
}

const candidates =
  process.platform === "win32"
    ? [["py", ["-3"]], ["python", []], ["python3", []]]
    : [["python3", []], ["python", []]];

for (const [command, prefix] of candidates) {
  const result = spawnSync(command, [...prefix, path.join(here, `${script}.py`), ...args], {
    stdio: "inherit",
    windowsHide: true,
    env: { ...process.env, PYTHONUTF8: "1" },
  });
  if (result.error && result.error.code === "ENOENT") continue;
  // Windows' "python" placeholder that opens the Microsoft Store exits with 9009 (or 49 from Git Bash)
  if (process.platform === "win32" && (result.status === 9009 || result.status === 49)) continue;
  process.exit(result.status ?? 1);
}

process.stderr.write("Python 3.9 or later wasn't found. Install it (python.org), then restart Claude Code.\n");
process.exit(127);
