import { copy } from "../src/copy";
import { S0_GAME_CODE, expect, expectNoAxeViolations, openS0Game, test } from "./fixtures";

// E2E-01 Join and lobby (document 15, section 9), the steps of S0-05's stories: 3, 4 (phone side) and 5. Steps 1, 2
// and 6 to 8 are added by the subplans that own US-03, US-05, US-07, US-08, US-09 and US-38.
// TODO(US-59): a game from DS-03 in Created, opened by the admin, instead of the e2e profile's S0 game.

const joinLink = `/join?code=${S0_GAME_CODE}`;

test.beforeEach(async ({ request }) => {
  await openS0Game(request);
});

test('AC-US01-01 AC-US04-01 E2E-01 step 3: phone A sees the privacy note, joins as "Priya" and waits in the lobby', async ({
  page,
  expectMessage,
}) => {
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

test('AC-US02-01 E2E-01 step 4: phone B types extra spaces around and inside "Priya S" and joins as "Priya S"', async ({
  newPhone,
}) => {
  const phoneB = await newPhone();
  await phoneB.goto(joinLink);

  await phoneB.getByRole("textbox", { name: copy.join.title }).fill("  Priya   S ");
  await phoneB.getByRole("button", { name: copy.join.submit }).click();

  await expect(
    phoneB.getByRole("heading", { level: 1, name: copy.lobby.welcome("Priya S") }),
  ).toBeVisible();
  await expect(phoneB.getByText(copy.lobby.waiting, { exact: true })).toBeVisible();
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
