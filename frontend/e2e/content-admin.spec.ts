import { copy } from "../src/copy";
import { expect, expectNoAxeViolations, loginAsAdmin, test } from "./fixtures";

// E2E-04 Content administration (document 15, section 9): step 1, from S1-03 (US-49). Steps 2 to 7 are added by the
// subplans that own the task, character and run plan editors (S2-07 owns the spec). A wrong password isn't tried
// here: failures count per address for 15 minutes (US-50), so repeated runs would block the specs' own logins.

test("AC-US49-01 E2E-04 step 1: the admin logs in with the correct password and the admin panel opens", async ({
  page,
  context,
}) => {
  await page.goto("/admin/login/");
  await expect(
    page.getByRole("heading", { level: 1, name: copy.admin.login.heading }),
  ).toBeVisible();
  await expectNoAxeViolations(page);

  await loginAsAdmin(page);

  await expect(page).toHaveURL(/\/admin\/$/);
  const session = (await context.cookies()).find((cookie) => cookie.name === "DH_SESSION");
  expect(session?.httpOnly).toBe(true);
  expect(session?.secure).toBe(true);
  expect(session?.sameSite).toBe("Strict");
});
