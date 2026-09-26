import AxeBuilder from "@axe-core/playwright";
import { expect, test as base, type APIRequestContext, type Page } from "@playwright/test";

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
  /** A new phone: its own browser context and empty storage, with the same checks as `page`. */
  newPhone: () => Promise<Page>;
};

async function blockOutsideRequests(page: Page, origin: string, blocked: string[]): Promise<void> {
  await page.route("**/*", async (route) => {
    const url = route.request().url();
    if (new URL(url).origin === origin) {
      await route.continue();
    } else {
      blocked.push(url);
      await route.abort("blockedbyclient");
    }
  });
}

async function listenForCspViolations(page: Page, violations: string[]): Promise<void> {
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
}

function originOf(baseURL: string | undefined): string {
  return new URL(baseURL ?? "http://localhost:8080").origin;
}

export const test = base.extend<Fixtures>({
  outsideRequests: [
    async ({ page, baseURL }, use) => {
      const blocked: string[] = [];
      await blockOutsideRequests(page, originOf(baseURL), blocked);
      await use(blocked);
      expect(blocked, "requests to other sites (X-07, NFR-24)").toEqual([]);
    },
    { auto: true },
  ],

  cspViolations: [
    async ({ page }, use) => {
      const violations: string[] = [];
      await listenForCspViolations(page, violations);
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

  newPhone: async ({ browser, baseURL, outsideRequests, cspViolations }, use) => {
    const contexts: Awaited<ReturnType<typeof browser.newContext>>[] = [];
    await use(async () => {
      const context = await browser.newContext({ baseURL });
      contexts.push(context);
      const phone = await context.newPage();
      await blockOutsideRequests(phone, originOf(baseURL), outsideRequests);
      await listenForCspViolations(phone, cspViolations);
      return phone;
    });
    await Promise.all(contexts.map((context) => context.close()));
  },
});

/** The join code of the S0 game (DS-02). */
export const S0_GAME_CODE = "K7PQ2M";

/**
 * Opens a fresh game in LOBBY with code K7PQ2M through the e2e profile's endpoint, discarding any earlier one, so a
 * spec always starts with no players (DI-08, DI-24). TODO(US-59): create the game through the admin panel (S1-04 T6).
 */
export async function openS0Game(request: APIRequestContext): Promise<void> {
  const response = await request.post("/api/test/s0-game");
  expect(response.status(), "the backend runs with DH_PROFILE=e2e").toBe(201);
}

/** Fails on any detectable WCAG 2.2 A or AA violation (AC-EN09-01, DEC-176). */
export async function expectNoAxeViolations(page: Page): Promise<void> {
  const results = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22a", "wcag22aa"])
    .analyze();
  expect(results.violations, "WCAG 2.2 A and AA violations").toEqual([]);
}

export { expect };
