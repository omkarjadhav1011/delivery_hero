import type { APIResponse, Response } from "@playwright/test";
import { expect, test } from "./fixtures";

// E2E-07 security-privacy (document 15, section 9). Step 1 checks the headers, and the shared fixtures fail every
// test on a CSP violation (LLD section 6.6) or a request to another site. Step 3 is the same-origin check below.
// TODO(US-10): step 2, DS-05 on a phone through practice, on the projector and in the admin panel (S1-02 T9)

// The four headers of AC-EN06-01 (NFR-19, NFR-20). API responses repeat nosniff and no-referrer (backend and Nginx),
// which Playwright joins with ", ", so every value must be exact: "no-referrer-when-downgrade" would be weaker.
function expectSecurityHeaders(response: Response | APIResponse, what: string): void {
  const headers = response.headers();
  const values = (name: string) => (headers[name] ?? "").split(",").map((value) => value.trim());
  expect(headers["content-security-policy"], `CSP on ${what}`).toBeDefined();
  expect(headers["content-security-policy"], `frame-ancestors on ${what}`).toContain(
    "frame-ancestors 'none'",
  );
  expect(new Set(values("x-content-type-options")), `nosniff on ${what}`).toEqual(
    new Set(["nosniff"]),
  );
  expect(new Set(values("referrer-policy")), `referrer policy on ${what}`).toEqual(
    new Set(["no-referrer"]),
  );
}

// The home, join, projector and admin pages (AC-EN08-02, TC-EN08-02)
const pages = ["/", "/join/?code=TEST", "/screen/?key=TEST", "/admin/login/", "/admin/"];

for (const path of pages) {
  test(`AC-EN06-01 every response loading ${path} carries the security headers`, async ({
    page,
  }) => {
    const responses: Response[] = [];
    page.on("response", (response) => responses.push(response));

    const document = await page.goto(path);
    await page.waitForLoadState("networkidle");

    expect(document).not.toBeNull();
    // The page's policy allows its own scripts only: no 'unsafe-inline' (DEC-135)
    const policy = document?.headers()["content-security-policy"] ?? "";
    expect(policy).toMatch(/script-src 'self'( 'sha256-[A-Za-z0-9+/=]+')*(;|$)/);
    expect(
      policy.split(";").find((directive) => directive.trim().startsWith("script-src")),
    ).not.toContain("unsafe-inline");
    for (const response of responses) {
      expectSecurityHeaders(response, response.url());
    }
  });

  test(`AC-EN08-02 every font, image and script on ${path} comes from the game's own address`, async ({
    page,
    baseURL,
  }) => {
    const origin = new URL(baseURL ?? "http://localhost:8080").origin;
    // An explicit log beside the shared outside-request blocker, which fails the test on any other site
    const assets: { type: string; url: string }[] = [];
    page.on("request", (request) => {
      const type = request.resourceType();
      if (type === "font" || type === "image" || type === "script") {
        assets.push({ type, url: request.url() });
      }
    });

    await page.goto(path);
    await page.waitForLoadState("networkidle");

    // The pixel font and the page's scripts load, so the log isn't empty by accident
    expect(assets.some((asset) => asset.type === "font")).toBe(true);
    expect(assets.some((asset) => asset.type === "script")).toBe(true);
    for (const asset of assets) {
      expect(new URL(asset.url).origin, `${asset.type} ${asset.url}`).toBe(origin);
    }
  });
}

test("AC-EN06-01 API and health responses carry the security headers", async ({ request }) => {
  expectSecurityHeaders(await request.get("/api/games/XXXXXX"), "a game status");
  expectSecurityHeaders(
    await request.post("/api/games/XXXXXX/players", { data: { name: "Priya" } }),
    "a join",
  );
  expectSecurityHeaders(await request.get("/api/admin/session"), "an admin request");
  expectSecurityHeaders(await request.get("/health"), "health");
});
