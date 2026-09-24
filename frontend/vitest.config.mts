import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

// Unit and component tests (document 13, section 5.1; document 14, section 5.2). The JUnit report carries the
// test names, including criterion IDs, for tools/ac_coverage.py (DEC-196).
export default defineConfig({
  resolve: {
    alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) },
  },
  test: {
    environment: "jsdom",
    include: ["src/**/*.test.{ts,tsx}"],
    reporters: ["default", ["junit", { outputFile: "test-results/vitest-junit.xml" }]],
    coverage: {
      provider: "v8",
      include: ["src/time/**/*.ts", "src/*/store.ts"],
      exclude: ["**/*.test.*"],
      reporter: ["text", "html"],
      reportsDirectory: "coverage",
      // 80% line coverage for client timing logic and the stores (CS-02)
      thresholds: {
        "src/time/**": { lines: 80 },
        "src/*/store.ts": { lines: 80 },
      },
    },
  },
});
