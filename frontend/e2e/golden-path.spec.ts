import { copy } from "../src/copy";
import type { GameView } from "../src/types/dto";
import {
  createGame,
  expect,
  expectNoAxeViolations,
  hostAction,
  projectorPath,
  test,
} from "./fixtures";

// E2E-02 Golden path (document 15, section 9): step 2, the countdown. The other steps are added by the subplans that
// own them. The game comes from the Quick 3-minute plan, created through the admin API, until S2-09 builds DS-03
// (DI-24); the steps wait on what the screens show, never on fixed times.

let game: GameView;

test.beforeEach(async ({ request }) => {
  game = await createGame(request);
  const opened = await hostAction(request, game.id, { action: "OPEN_LOBBY" });
  expect(opened.status(), "opening the lobby").toBe(200);
});

/** m:ss, as the projector's clock shows the time left. */
const CLOCK = /^\d:\d\d$/;

test("AC-US13-01 AC-US13-03 AC-US04-02 E2E-02 step 2: the host starts the round; the phone and the projector count down together, the phone switches by itself, and the projector shows the clock and the phase bar", async ({
  page: screen,
  request,
  newPhone,
}) => {
  await screen.goto(projectorPath(game));
  const phone = await newPhone();
  await phone.goto(`/join?code=${game.code}`);
  await phone.getByRole("textbox", { name: copy.join.title }).fill("Sam");
  await phone.getByRole("button", { name: copy.join.submit }).click();
  await expect(
    phone.getByRole("heading", { level: 1, name: copy.lobby.welcome("Sam") }),
  ).toBeVisible();
  await expect(screen.getByText(copy.screen.joined(1), { exact: true })).toBeVisible();

  const started = await hostAction(request, game.id, { action: "START_ROUND" });
  expect(started.status(), "starting the round").toBe(200);

  // Both count down from the broadcast start, with no input on the phone
  await expect(phone.getByRole("heading", { level: 1, name: copy.countdown.title })).toBeVisible();
  await expect(phone.getByText(copy.countdown.tagline, { exact: true })).toBeVisible();
  await expect(phone.getByTestId("countdown-digit")).toHaveText(/^[1-5]$/);
  await expect(
    screen.getByRole("heading", { level: 1, name: copy.screen.sprintStarts }),
  ).toBeVisible();
  await expect(screen.getByText(/^[1-5]$/)).toBeVisible();
  await expectNoAxeViolations(phone);
  await expectNoAxeViolations(screen);

  // The round starts at the broadcast start time, within the 5-second countdown of it being seen: the projector's
  // live header replaces the countdown
  const phaseBar = screen.getByRole("list", { name: copy.screen.phaseBarLabel });
  await expect(phaseBar).toBeVisible({ timeout: 7_000 });
  await expect(phaseBar.getByRole("listitem")).toHaveText([
    copy.screen.phases.PLANNING,
    copy.screen.phases.DEVELOPMENT,
    copy.screen.phases.TESTING,
    copy.screen.phases.RELEASE,
  ]);
  await expect(phaseBar.locator('[aria-current="step"]')).toHaveText(copy.screen.phases.PLANNING);
  const clock = screen.getByRole("banner").getByText(CLOCK);
  await expect(clock).toBeVisible();
  const first = await clock.textContent();
  await expect(clock).not.toHaveText(first ?? "");
  // The phone leaves the countdown by itself; its task screen arrives with S1-09
  await expect(phone.getByRole("heading", { level: 1, name: copy.countdown.title })).toHaveCount(0);
  await expectNoAxeViolations(screen);
});
