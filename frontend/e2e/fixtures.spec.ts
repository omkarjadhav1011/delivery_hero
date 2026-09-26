import { expect, test } from "./fixtures";

// The shared fixtures catch what they should (document 15, section 9).

test("the outside-request blocker catches and records a request to another site (X-07, NFR-24)", async ({
  page,
  outsideRequests,
}) => {
  await page.goto("/");

  // A top-level navigation: the content security policy doesn't cover it, so only the blocker can stop it
  const navigation = await page.goto("https://example.org/").catch((error: unknown) => error);

  expect(navigation).toBeInstanceOf(Error);
  expect(outsideRequests).toEqual(["https://example.org/"]);
  // Proven; empty the list so the fixture's own end-of-test check passes
  outsideRequests.length = 0;
});
