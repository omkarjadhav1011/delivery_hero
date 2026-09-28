import AxeBuilder from "@axe-core/playwright";
import {
  expect,
  test as base,
  type APIRequestContext,
  type APIResponse,
  type Page,
} from "@playwright/test";
import { baseURL } from "../../playwright.config";
import { copy } from "../../src/copy";
import type { GameView, HostActionRequest, RunPlanSummary } from "../../src/types/dto";

// Shared fixtures for every end-to-end spec (document 15, section 9):
// - the outside-request blocker fails a test on any request to another site (X-07, NFR-24);
// - the exact-message helper checks wording against src/copy.ts (X-01);
// - the CSP-violation listener fails a test on any content security policy violation (NFR-19).

/** The admin password for specs that sign in: the local-only password unless E2E_ADMIN_PASSWORD is set (SG-05). */
export const adminPassword = process.env.E2E_ADMIN_PASSWORD ?? "DHAdmin";

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

/** The run plan the specs play until DS-03 exists (DI-24, S2-09). */
export const QUICK_PLAN = "quick-3min";

/** The CSRF token the last response set, for the next state-changing request (document 11, section 5.2). */
async function csrfHeader(request: APIRequestContext): Promise<Record<string, string>> {
  const cookies = (await request.storageState()).cookies;
  const token = cookies.find((cookie) => cookie.name === "XSRF-TOKEN")?.value;
  return token === undefined ? {} : { "X-XSRF-TOKEN": token };
}

/** Logs the request context in as the admin, through the same endpoints as A-01 (document 11, section 7.3). */
export async function adminApi(request: APIRequestContext): Promise<APIRequestContext> {
  await request.get("/api/admin/session");
  const login = await request.post("/api/admin/login", {
    form: { username: "admin", password: adminPassword },
    headers: await csrfHeader(request),
  });
  expect(login.ok(), "the admin login with the e2e password").toBe(true);
  // The session check hands out the token for the new session
  await request.get("/api/admin/session");
  return request;
}

/** Sends a host action as the logged-in admin (document 11, section 7.8). */
export async function hostAction(
  request: APIRequestContext,
  gameId: string,
  body: HostActionRequest,
): Promise<APIResponse> {
  return request.post(`/api/admin/games/${gameId}/actions`, {
    data: body,
    headers: await csrfHeader(request),
  });
}

/** The open game, or null (document 11, section 7.7). */
export async function currentGame(request: APIRequestContext): Promise<GameView | null> {
  const response = await request.get("/api/admin/games/current");
  return response.status() === 204 ? null : ((await response.json()) as GameView);
}

/**
 * Cancels the open game, if there is one, so the spec can create its own: only one game is open at a time (DEC-101).
 * A game in Results can't be cancelled, and closing it is US-65 (S2-04).
 */
export async function cancelOpenGame(request: APIRequestContext): Promise<void> {
  const open = await currentGame(request);
  if (open === null) {
    return;
  }
  // Never a real game on another environment: a test run must not end a game in progress
  const local = /^http:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/.test(baseURL);
  expect(
    local || open.test,
    `refusing to cancel game ${open.code} on ${baseURL}: it isn't a test game`,
  ).toBe(true);
  const cancelled = await hostAction(request, open.id, { action: "CANCEL", confirm: true });
  expect(cancelled.status(), `cancelling the open game in ${open.state}`).toBe(200);
}

/**
 * Creates a game from the Quick 3-minute plan through the admin API, after cancelling any open one, and opens its
 * lobby, so a spec always starts with an empty lobby (DI-24). Returns the game view.
 */
export async function openGameInLobby(request: APIRequestContext): Promise<GameView> {
  await adminApi(request);
  await cancelOpenGame(request);
  const plans = (await (await request.get("/api/admin/run-plans")).json()) as RunPlanSummary[];
  const plan = plans.find((candidate) => candidate.key === QUICK_PLAN);
  expect(plan, "the task pool is loaded (Setup Guide, section 10.4)").toBeDefined();
  const created = await request.post("/api/admin/games", {
    data: { runPlanId: plan?.id },
    headers: await csrfHeader(request),
  });
  expect(created.status(), "creating the game").toBe(201);
  const game = (await created.json()) as GameView;
  const opened = await hostAction(request, game.id, { action: "OPEN_LOBBY" });
  expect(opened.status(), "opening the lobby").toBe(200);
  return { ...game, state: "LOBBY" };
}

/**
 * Logs in on A-01 with the admin password, as E2E-04 step 1 does, and waits for the admin panel (AC-US49-01). The
 * later admin specs start with it.
 */
export async function loginAsAdmin(page: Page): Promise<void> {
  await page.goto("/admin/login/");
  await page.getByLabel(copy.admin.login.password).fill(adminPassword);
  await page.getByRole("button", { name: copy.admin.login.submit }).click();
  await expect(page.getByRole("navigation", { name: copy.admin.navLabel })).toBeVisible();
}

/** Fails on any detectable WCAG 2.2 A or AA violation (AC-EN09-01, DEC-176). */
export async function expectNoAxeViolations(page: Page): Promise<void> {
  const results = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22a", "wcag22aa"])
    .analyze();
  expect(results.violations, "WCAG 2.2 A and AA violations").toEqual([]);
}

export { expect };
