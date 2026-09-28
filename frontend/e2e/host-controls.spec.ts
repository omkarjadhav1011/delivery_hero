import type { Page } from "@playwright/test";
import { copy } from "../src/copy";
import {
  adminApi,
  cancelOpenGame,
  currentGame,
  expect,
  expectNoAxeViolations,
  loginAsAdmin,
  openGameInLobby,
  test,
} from "./fixtures";

// E2E-03 Host controls (document 15, section 9), steps 1 to 5, with two admins and two phones on a game from the Quick
// 3-minute plan (DI-24). Steps 6 (void) and 7 (cancel for everyone) are added by S2-23. The per-task answer counts
// stay at 0 until phones can answer (S1-12, S1-13).

const text = copy.admin.liveControl;

function headerOf(page: Page, code: string, state: string) {
  return page.getByRole("heading", { name: text.header(code, "Quick 3-minute plan", state) });
}

async function join(phone: Page, code: string, name: string): Promise<void> {
  await phone.goto(`/join?code=${code}`);
  await phone.getByRole("textbox", { name: copy.join.title }).fill(name);
  await phone.getByRole("button", { name: copy.join.submit }).click();
  await expect(
    phone.getByRole("heading", { level: 1, name: copy.lobby.welcome(name) }),
  ).toBeVisible();
}

test.afterEach(async ({ request }) => {
  // Frees the one open game slot for the next spec (DEC-101)
  await cancelOpenGame(await adminApi(request));
});

test("AC-US59-03 AC-US60-02 AC-US60-03 AC-US60-04 AC-US60-05 E2E-03 steps 1 to 5: host controls", async ({
  page: adminA,
  request,
  newPhone,
}) => {
  test.setTimeout(60_000);
  await cancelOpenGame(await adminApi(request));
  await loginAsAdmin(adminA);
  await adminA.goto("/admin/games/");
  await expect(adminA.getByRole("button", { name: copy.admin.newGameScreen.create })).toBeEnabled();

  // Step 1: another admin opens a game first; A's Create game is refused, and A is shown the open game
  const game = await openGameInLobby(request);
  await adminA.getByRole("button", { name: copy.admin.newGameScreen.create }).click();
  await expect(adminA.getByText(copy.admin.newGameScreen.anotherGameOpen)).toBeVisible();
  await expect(headerOf(adminA, game.code, "LOBBY")).toBeVisible();
  await expect(adminA.getByText(game.code, { exact: true })).toBeVisible();
  await expectNoAxeViolations(adminA);

  // Step 2: Cancel asks first, and nothing happens without confirmation
  await adminA.getByRole("button", { name: text.cancelGame }).click();
  const dialog = adminA.getByRole("dialog", { name: text.confirmCancel });
  await expect(dialog).toBeVisible();
  await expectNoAxeViolations(adminA);
  await dialog.getByRole("button", { name: text.keepGame }).click();
  await expect(dialog).toBeHidden();
  expect((await currentGame(request))?.state).toBe("LOBBY");

  // Two phones join; A's screen hears it from LIVE_STATS and offers Start round
  const phoneA = await newPhone();
  const phoneB = await newPhone();
  await join(phoneA, game.code, "Priya");
  await join(phoneB, game.code, "Sam");
  await expect(adminA.getByText(text.players(2, 2, 0))).toBeVisible();
  await expect(adminA.getByRole("button", { name: text.startRound })).toBeEnabled();

  // Admin B, and B's second tab, whose connection is down, so it still shows the lobby later (step 4)
  const adminB = await newPhone();
  await loginAsAdmin(adminB);
  await adminB.goto("/admin/games/");
  await expect(adminB.getByRole("button", { name: text.startRound })).toBeEnabled();
  const staleB = await newPhone();
  await staleB.routeWebSocket(/\/ws$/, () => {
    // Never connected: the tab gets no LIVE_STATS
  });
  await loginAsAdmin(staleB);
  await staleB.goto("/admin/games/");
  await expect(staleB.getByRole("button", { name: text.startPractice })).toBeEnabled();

  // Step 3: A and B press Start round together; the round starts once and neither sees an error
  await Promise.all([
    adminA.getByRole("button", { name: text.startRound }).click(),
    adminB.getByRole("button", { name: text.startRound }).click(),
  ]);
  for (const admin of [adminA, adminB]) {
    await expect(admin.getByRole("button", { name: text.startRound })).toBeDisabled();
    await expect(admin.getByText(text.failed)).toBeHidden();
  }
  const started = await currentGame(request);
  expect(["COUNTDOWN", "LIVE"]).toContain(started?.state);

  // Step 4: B's stale tab sends Start practice; nothing changes, and the tab refreshes with no error
  await staleB.getByRole("button", { name: text.startPractice }).click();
  await expect(staleB.getByRole("button", { name: text.startPractice })).toBeDisabled();
  await expect(headerOf(staleB, game.code, "LOBBY")).toBeHidden();
  await expect(
    staleB.getByRole("heading", {
      name: new RegExp(`^Game ${game.code} · Quick 3-minute plan · (COUNTDOWN|LIVE)$`),
    }),
  ).toBeVisible();
  await expect(staleB.getByText(text.failed)).toBeHidden();
  expect(["COUNTDOWN", "LIVE"]).toContain((await currentGame(request))?.state);

  // Step 5: during the round, A sees the state, time left, players, incident status and the scored tasks
  await expect(headerOf(adminA, game.code, "LIVE")).toBeVisible({ timeout: 10_000 });
  await expect(adminA.getByText(/^\d:\d\d left$/)).toBeVisible();
  await expect(adminA.getByText(text.players(2, 2, 0))).toBeVisible();
  await expect(adminA.getByText(text.incident.PENDING)).toBeVisible();
  await expect(adminA.getByRole("heading", { name: text.tasks })).toBeVisible();
  const tasks = adminA.getByRole("list", { name: text.tasks });
  await expect(tasks.getByRole("listitem").first()).toContainText(text.answers(0));
  await expect(tasks.getByRole("listitem").first()).toContainText(text.wrong(0));
  await expect(adminA.getByRole("button", { name: text.cancelGame })).toBeEnabled();
  await expectNoAxeViolations(adminA);
});
