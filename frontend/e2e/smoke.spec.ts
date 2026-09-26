import { copy } from "../src/copy";
import { expect, expectNoAxeViolations, test } from "./fixtures";

// The local stack starts and serves the static site through Nginx (AC-EN01-01, TC-EN01-01; Setup Guide section 6).

test("AC-EN01-01 the home page loads with no accessibility or CSP violations", async ({
  page,
  expectMessage,
}) => {
  const response = await page.goto("/");

  expect(response?.status()).toBe(200);
  expect(response?.headers()["content-security-policy"]).toContain("script-src 'self'");
  await expectMessage(copy.home.title);
  await expectNoAxeViolations(page);
});

test("AC-EN01-01 the join page loads with no accessibility or CSP violations", async ({
  page,
  expectMessage,
}) => {
  const response = await page.goto("/join/?code=TEST");

  expect(response?.status()).toBe(200);
  // No game has that code, so the join page shows the inactive-link message (P-03)
  await expectMessage(copy.joinMessages.GAME_NOT_ACTIVE);
  await expectNoAxeViolations(page);
});

// Every other page of LLD section 6.1 loads with the CSP header applied (LLD section 6.6)
const otherPages = [
  "/screen/?key=TEST",
  "/admin/",
  "/admin/login/",
  "/admin/games/",
  "/admin/tasks/",
  "/admin/tasks/edit/",
  "/admin/run-plans/",
  "/admin/run-plans/edit/",
  "/admin/characters/",
  "/admin/past-games/",
];

for (const path of otherPages) {
  test(`AC-EN01-01 ${path} loads with no accessibility or CSP violations`, async ({ page }) => {
    const response = await page.goto(path);

    expect(response?.status()).toBe(200);
    expect(response?.headers()["content-security-policy"]).toContain("script-src 'self'");
    await expectNoAxeViolations(page);
  });
}

test("AC-EN01-01 the health endpoint reports UP", async ({ request }) => {
  const response = await request.get("/health");

  expect(response.status()).toBe(200);
  expect(await response.json()).toEqual({ status: "UP" });
});
