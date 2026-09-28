import { copy } from "../src/copy";
import {
  adminApi,
  cancelOpenGame,
  expect,
  expectNoAxeViolations,
  loginAsAdmin,
  test,
} from "./fixtures";

// A-08 New game (S1-04 T5): the screen's axe check, with no game open. Creating a game is E2E-03 `host-controls`.

test("A-08 New game lists the seed's run plans as ready, with no axe violations", async ({
  page,
  request,
}) => {
  // Another spec's game would show A-09 instead of the form (DEC-101)
  await cancelOpenGame(await adminApi(request));
  await loginAsAdmin(page);
  await page.goto("/admin/games/");

  await expect(page.getByRole("heading", { level: 1, name: copy.admin.newGame })).toBeVisible();
  await expect(page.getByLabel(copy.admin.newGameScreen.runPlan)).toBeVisible();
  await expect(page.getByText(copy.admin.newGameScreen.ready)).toBeVisible();
  await expectNoAxeViolations(page);
});
