import { copy } from "../src/copy";
import { expect, expectNoAxeViolations, loginAsAdmin, test } from "./fixtures";

// A-08 New game (S1-04 T5): the screen's axe check. Creating a game is E2E-03 `host-controls` (S1-07), which cancels
// what it creates; this spec creates nothing, so the other specs keep the one open game slot (DEC-101).

test("A-08 New game lists the seed's run plans as ready, with no axe violations", async ({
  page,
}) => {
  await loginAsAdmin(page);
  await page.goto("/admin/games/");

  await expect(page.getByRole("heading", { level: 1, name: copy.admin.newGame })).toBeVisible();
  await expect(page.getByLabel(copy.admin.newGameScreen.runPlan)).toBeVisible();
  await expect(page.getByText(copy.admin.newGameScreen.ready)).toBeVisible();
  await expectNoAxeViolations(page);
});
