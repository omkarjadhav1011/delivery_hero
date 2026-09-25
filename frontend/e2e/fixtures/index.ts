import AxeBuilder from "@axe-core/playwright";
import { expect, test as base, type Page } from "@playwright/test";

// Shared fixtures for every end-to-end spec (document 15, section 9):
// - the outside-request blocker fails a test on any request to another site (X-07, NFR-24);
// - the exact-message helper checks wording against src/copy.ts (X-01);
// - the CSP-violation listener fails a test on any content security policy violation (NFR-19).

/** The admin password for specs that sign in: the local-only password unless E2E_ADMIN_PASSWORD is set (SG-05). */
export const adminPassword = process.env.E2E_ADMIN_PASSWORD ?? "delivery-hero-local";

type Fixtures = {
  outsideRequests: string[];
  cspViolations: string[];
  expectMessage: (text: string) => Promise<void>;
};

export const test = base.extend<Fixtures>({
  outsideRequests: [
    async ({ page, baseURL }, use) => {
      const origin = new URL(baseURL ?? "http://localhost:8080").origin;
      const blocked: string[] = [];
      await page.route("**/*", async (route) => {
        const url = route.request().url();
        if (new URL(url).origin === origin) {
          await route.continue();
        } else {
          blocked.push(url);
          await route.abort("blockedbyclient");
        }
      });
      await use(blocked);
      expect(blocked, "requests to other sites (X-07, NFR-24)").toEqual([]);
    },
    { auto: true },
  ],

  cspViolations: [
    async ({ page }, use) => {
      const violations: string[] = [];
      await page.exposeFunction("__reportCspViolation", (violation: string) => {
        violations.push(violation);
      });
      await page.addInitScript(() => {
        document.addEventListener("securitypolicyviolation", (event) => {
          const report = (window as unknown as { __reportCspViolation: (v: string) => void })
            .__reportCspViolation;
          report(`${event.effectiveDirective} blocked ${event.blockedURI || "inline code"}`);
        });
      });
      page.on("console", (message) => {
        if (message.type() === "error" && /Content Security Policy/i.test(message.text())) {
          violations.push(message.text());
        }
      });
      await use(violations);
      expect(violations, "content security policy violations (NFR-19)").toEqual([]);
    },
    { auto: true },
  ],

  expectMessage: async ({ page }, use) => {
    await use(async (text: string) => {
      await expect(page.getByText(text, { exact: true })).toBeVisible();
    });
  },
});

/** Fails on any detectable WCAG 2.2 A or AA violation (AC-EN09-01, DEC-176). */
export async function expectNoAxeViolations(page: Page): Promise<void> {
  const results = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22a", "wcag22aa"])
    .analyze();
  expect(results.violations, "WCAG 2.2 A and AA violations").toEqual([]);
}

export { expect };
