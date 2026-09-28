import type { Page } from "@playwright/test";
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

// E2E-01 Join and lobby (document 15, section 9): steps 2, 3, 4 and 5. Steps 1 and 6 to 8 are added by the subplans
// that own US-03, US-05, US-07, US-08 and US-09.
// Each test starts with a new game from the Quick 3-minute plan, created through the admin API (DI-24).

let game: GameView;
let joinLink = "";

test.beforeEach(async ({ request }) => {
  game = await createGame(request);
  joinLink = `/join?code=${game.code}`;
});

async function openLobby(request: Parameters<typeof hostAction>[0]): Promise<void> {
  const opened = await hostAction(request, game.id, { action: "OPEN_LOBBY" });
  expect(opened.status(), "opening the lobby").toBe(200);
}

/** The projector's list of joined names, newest first. */
function projectorNames(screen: Page) {
  return screen.getByRole("listitem");
}

test('AC-US38-01 E2E-01 step 2: the admin opens the lobby, and the projector shows the QR code, the URL, "Open this link in Chrome" and a count of 0', async ({
  page: screen,
  request,
}) => {
  await screen.goto(projectorPath(game));
  await expect(screen.getByText(copy.screen.gettingReady, { exact: true })).toBeVisible();

  await openLobby(request);

  const qr = screen.getByRole("img", { name: copy.screen.qrLabel });
  await expect(qr).toBeVisible();
  const box = await qr.boundingBox();
  expect(box?.width ?? 0).toBeGreaterThanOrEqual(400);
  expect(box?.height ?? 0).toBeGreaterThanOrEqual(400);
  await expect(
    screen.getByText(new URL(joinLink, game.projectorUrl).href, { exact: true }),
  ).toBeVisible();
  await expect(screen.getByText(copy.screen.openInChrome, { exact: true })).toBeVisible();
  await expect(screen.getByText(copy.screen.joined(0), { exact: true })).toBeVisible();
  await expect(projectorNames(screen)).toHaveCount(0);
  await expectNoAxeViolations(screen);
});

test('AC-US01-01 AC-US04-01 E2E-01 step 3: phone A sees the privacy note, joins as "Priya" and waits in the lobby', async ({
  page,
  request,
  expectMessage,
}) => {
  await openLobby(request);
  await page.goto(joinLink);

  const field = page.getByRole("textbox", { name: copy.join.title });
  await expect(field).toBeVisible();
  await expectMessage(copy.join.privacy);
  await expectNoAxeViolations(page);
  await field.fill("Priya");
  await page.getByRole("button", { name: copy.join.submit }).click();

  await expect(
    page.getByRole("heading", { level: 1, name: copy.lobby.welcome("Priya") }),
  ).toBeVisible();
  await expectMessage(copy.lobby.waiting);
  await expectNoAxeViolations(page);
});

test('AC-US02-01 E2E-01 step 4: phone B types extra spaces around and inside "Priya S", joins as "Priya S", and the projector lists it before "Priya"', async ({
  page: screen,
  request,
  newPhone,
}) => {
  await openLobby(request);
  await screen.goto(projectorPath(game));
  await expect(screen.getByText(copy.screen.joined(0), { exact: true })).toBeVisible();
  const phoneA = await newPhone();
  await phoneA.goto(joinLink);
  await phoneA.getByRole("textbox", { name: copy.join.title }).fill("Priya");
  await phoneA.getByRole("button", { name: copy.join.submit }).click();
  await expect(
    phoneA.getByRole("heading", { level: 1, name: copy.lobby.welcome("Priya") }),
  ).toBeVisible();

  const phoneB = await newPhone();
  await phoneB.goto(joinLink);
  await phoneB.getByRole("textbox", { name: copy.join.title }).fill("  Priya   S ");
  await phoneB.getByRole("button", { name: copy.join.submit }).click();

  await expect(
    phoneB.getByRole("heading", { level: 1, name: copy.lobby.welcome("Priya S") }),
  ).toBeVisible();
  await expect(phoneB.getByText(copy.lobby.waiting, { exact: true })).toBeVisible();
  // The projector hears both joins without a reload, newest first
  await expect(screen.getByText(copy.screen.joined(2), { exact: true })).toBeVisible();
  await expect(projectorNames(screen)).toHaveText(["Priya S", "Priya"]);
});

test("AC-US01-03 E2E-01 step 5: a phone with an inactive code sees the inactive-link message", async ({
  newPhone,
}) => {
  const phone = await newPhone();

  await phone.goto("/join?code=ZZZZ22");

  await expect(phone.getByText(copy.joinMessages.GAME_NOT_ACTIVE, { exact: true })).toBeVisible();
  await expect(phone.getByRole("textbox")).toHaveCount(0);
  await expectNoAxeViolations(phone);
});
