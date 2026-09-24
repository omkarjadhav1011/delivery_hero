// Starts one of the Python hooks with whichever Python 3 this machine has (py -3, python or
// python3), passing stdin through and keeping the hook's exit code. Node runs the same way on
// Windows, macOS and Linux, so settings.json can call every hook through this file.
import { spawnSync } from "node:child_process";
import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const hook = process.argv[2] ?? "";
const guards = new Set(["guard_bash", "guard_files"]);

if (!/^[a-z_]+$/.test(hook)) {
  process.stderr.write(`run-hook.mjs: unknown hook "${hook}"\n`);
  process.exit(1);
}

let input;
try {
  input = readFileSync(0);
} catch {
  input = Buffer.from("{}");
}

const candidates =
  process.platform === "win32"
    ? [["py", ["-3"]], ["python", []], ["python3", []]]
    : [["python3", []], ["python", []]];

for (const [command, prefix] of candidates) {
  const result = spawnSync(command, [...prefix, path.join(here, `${hook}.py`)], {
    input,
    windowsHide: true,
    maxBuffer: 16 * 1024 * 1024,
  });
  if (result.error && result.error.code === "ENOENT") continue;
  // Windows' "python" placeholder that opens the Microsoft Store exits with 9009
  if (process.platform === "win32" && result.status === 9009) continue;
  if (result.stdout) process.stdout.write(result.stdout);
  if (result.stderr) process.stderr.write(result.stderr);
  process.exit(result.status ?? 1);
}

const advice = "Install Python 3.9 or later (python.org), then restart Claude Code. See .claude/README.md.";
if (guards.has(hook)) {
  // Fail closed: without Python the safety checks can't run, so nothing gets through unchecked
  process.stderr.write(`Blocked: the Claude Code safety hooks need Python 3, which wasn't found. ${advice}\n`);
  process.exit(2);
}
if (hook === "session_start") {
  process.stdout.write(`Python 3 wasn't found, so the project's Claude Code hooks aren't working. ${advice}\n`);
}
process.exit(0);
