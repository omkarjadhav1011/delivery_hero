import { defineConfig, devices } from "@playwright/test";

// End-to-end and accessibility tests (document 14 section 7.4, document 15 section 9, Setup Guide section 10.4).
// They run against the local stack with DH_PROFILE=e2e, or any environment named by E2E_BASE_URL (SG-05).
export const baseURL = process.env.E2E_BASE_URL ?? "http://localhost:8080";

export default defineConfig({
  testDir: "e2e",
  // Only one game can be open at a time (DEC-101), so specs run one after another
  fullyParallel: false,
  workers: 1,
  // One retry in CI, reported as flaky (TP-08)
  retries: process.env.CI ? 1 : 0,
  forbidOnly: !!process.env.CI,
  // Playwright empties its outputDir on every run; keep it apart from the JUnit reports in test-results/
  outputDir: "test-results/playwright-artifacts",
  reporter: [
    ["list"],
    ["junit", { outputFile: "test-results/playwright-junit.xml" }],
    ["html", { outputFolder: "playwright-report", open: "never" }],
  ],
  use: {
    baseURL,
    trace: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
