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
  await expectMessage(copy.join.title);
  await expectNoAxeViolations(page);
});

test("AC-EN01-01 the health endpoint reports UP", async ({ request }) => {
  const response = await request.get("/health");

  expect(response.status()).toBe(200);
  expect(await response.json()).toEqual({ status: "UP" });
});
