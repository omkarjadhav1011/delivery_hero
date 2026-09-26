import { copy } from "../src/copy";
import { randomUUID } from "node:crypto";
import { readFileSync } from "node:fs";
import type { Page } from "@playwright/test";
import { expect, expectNoAxeViolations, loginAsAdmin, test } from "./fixtures";

// E2E-04 Content administration (document 15, section 9): step 1, from S1-03 (US-49). Steps 2 to 7 are added by the
// subplans that own the task, character and run plan editors (S2-07 owns the spec). A wrong password isn't tried
// here: failures count per address for 15 minutes (US-50), so repeated runs would block the specs' own logins.

test("AC-US49-01 AC-US49-04 E2E-04 step 1: the admin logs in with the correct password, the admin panel opens and DH_SESSION has its flags", async ({
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

test("AC-US49-03 E2E-04 step 1: Log out ends the session, and the admin panel then asks to log in", async ({
  page,
}) => {
  await loginAsAdmin(page);

  await page.getByRole("button", { name: copy.admin.logout }).click();
  await expect(page).toHaveURL(/\/admin\/login\/$/);
  const session = await page.request.get("/api/admin/session");
  expect(await session.json()).toEqual({ authenticated: false, expiresAt: null });

  await page.goto("/admin/tasks/");
  await expect(page).toHaveURL(/\/admin\/login\/$/);
});

// Step 2, tasks (S2-07 T6). Keys carry a run suffix and each task is deleted at the end, so the spec can run again
// on the same database. The phone preview check arrives with T7; deleting mgr-plan-01 moves to S2-08 T2, where the
// library can find it.
const text = copy.admin.taskEditor;
const run = randomUUID().slice(0, 8);

async function openNewTask(page: Page, key: string, type: keyof typeof text.types): Promise<void> {
  await page.goto("/admin/tasks/edit/");
  await expect(page.getByRole("heading", { level: 1, name: text.newTask })).toBeVisible();
  await page.getByLabel(text.key).fill(key);
  await page.getByLabel(text.role).selectOption("TESTER");
  await page.getByLabel(text.phase).selectOption("TESTING");
  await page.getByLabel(text.type).selectOption(type);
  await page.getByLabel(text.prompt).fill("Which of these would you fix first?");
  await page.getByLabel(text.explanation).fill("Severity first.");
}

async function saveAndCheck(page: Page, key: string): Promise<void> {
  await page.getByRole("button", { name: text.save }).click();
  await expect(page.getByText(text.saved, { exact: true })).toBeVisible();
  await expect(page).toHaveURL(/\/admin\/tasks\/edit\/\?id=[0-9a-f-]{36}$/);
  await expect(
    page.getByRole("heading", { level: 1, name: `${copy.admin.editTask} ${key}` }),
  ).toBeVisible();
}

test("AC-US51-01 E2E-04 step 2: the admin creates one valid task of each type, then deletes each", async ({
  page,
}) => {
  await loginAsAdmin(page);
  const saved: string[] = [];

  await openNewTask(page, `e2e-mc-${run}`, "MULTIPLE_CHOICE");
  await expectNoAxeViolations(page);
  await page.getByLabel(text.optionText(1), { exact: true }).fill("Checkout fails for every user");
  await page.getByLabel(text.optionText(2), { exact: true }).fill("A typo in the footer");
  await saveAndCheck(page, `e2e-mc-${run}`);
  saved.push(page.url());

  await openNewTask(page, `e2e-yn-${run}`, "YES_NO");
  await expect(page.getByText(text.timeLimitHint(8), { exact: true })).toBeVisible();
  await page.getByLabel(text.no).check();
  await saveAndCheck(page, `e2e-yn-${run}`);
  saved.push(page.url());

  await openNewTask(page, `e2e-order-${run}`, "ORDER");
  await page.getByLabel(text.itemText(1), { exact: true }).fill("Integration test");
  await page.getByLabel(text.itemText(2), { exact: true }).fill("Unit test");
  await page.getByLabel(text.itemText(3), { exact: true }).fill("End-to-end test");
  await saveAndCheck(page, `e2e-order-${run}`);
  saved.push(page.url());

  await openNewTask(page, `e2e-words-${run}`, "PROBLEM_WORDS");
  await page
    .getByLabel(text.markedText)
    .fill("Warn when the balance is {{low}} for {{several}} days");
  await saveAndCheck(page, `e2e-words-${run}`);
  await expectNoAxeViolations(page);
  saved.push(page.url());

  for (const url of saved) {
    await page.goto(url);
    await expect(page.getByLabel(text.prompt)).toHaveValue("Which of these would you fix first?");
    await page.getByRole("button", { name: text.delete }).click();
    await expect(page).toHaveURL(/\/admin\/tasks\/$/);
  }
});

test("AC-US51-02 E2E-04 step 2: invalid saves are refused, each message naming the problem", async ({
  page,
}) => {
  await loginAsAdmin(page);

  // The correct-option radio allows only one, so two correct options can't be sent from here; TaskApiIT covers it
  await openNewTask(page, `e2e-order-bad-${run}`, "ORDER");
  await page.getByLabel(text.itemText(1), { exact: true }).fill("Unit test");
  await page.getByLabel(text.correctPosition(1)).fill("1");
  await page.getByLabel(text.itemText(2), { exact: true }).fill("Integration test");
  await page.getByLabel(text.correctPosition(2)).fill("2");
  await page.getByLabel(text.itemText(3), { exact: true }).fill("End-to-end test");
  await page.getByRole("button", { name: text.save }).click();
  await expect(
    page.getByText("The display order must differ from the correct order.", { exact: true }),
  ).toBeVisible();

  await page.getByLabel(text.type).selectOption("PROBLEM_WORDS");
  await page.getByLabel(text.markedText).fill("{{one}} {{two}} {{three}} {{four}} {{five}}");
  await page.getByRole("button", { name: text.save }).click();
  await expect(page.getByText("Mark 1 to 4 problem words.", { exact: true })).toBeVisible();

  await page.getByLabel(text.prompt).fill("");
  await page.getByRole("button", { name: text.save }).click();
  await expect(page.getByText("Must be 1 to 200 characters.", { exact: true })).toBeVisible();
  await expect(page.getByText(text.saved, { exact: true })).toHaveCount(0);
  await expect(page).toHaveURL(/\/admin\/tasks\/edit\/$/);
});

// Step 2, the library (S2-08 T2). It reads the seeded library (DS-01), which the e2e stack loads.

test("AC-US52-01 E2E-04 step 2: filtering the library by Tester and Tap to order gives exactly the three expected tasks", async ({
  page,
}) => {
  await loginAsAdmin(page);
  await page.goto("/admin/tasks/");
  await expect(page.getByRole("link", { name: "mgr-plan-01", exact: true })).toBeVisible();
  await expectNoAxeViolations(page);

  await page.getByLabel(text.role).selectOption("TESTER");
  await page.getByLabel(text.type).selectOption("ORDER");

  // The server lists the library by key (DI-64)
  const keys = page.getByRole("rowheader");
  await expect(keys).toHaveText(["tst-dev-03", "tst-rel-03", "tst-test-01"]);
  await expect(page.getByRole("row", { name: /tst-test-01/ })).toContainText(text.types.ORDER);
});

test("AC-US51-05 E2E-04 step 2: mgr-plan-01, opened from the library, can't be deleted and names both plans", async ({
  page,
}) => {
  await loginAsAdmin(page);
  await page.goto("/admin/tasks/");
  await page.getByRole("link", { name: "mgr-plan-01", exact: true }).click();

  await expect(
    page.getByRole("heading", { level: 1, name: `${copy.admin.editTask} mgr-plan-01` }),
  ).toBeVisible();
  await expect(page.getByRole("button", { name: text.delete })).toBeDisabled();
  await expect(
    page.getByText(text.usedBy(["Default 5-minute plan", "Quick 3-minute plan"]), { exact: true }),
  ).toBeVisible();
});

// Step 3, edit conflict (S2-08 T4). The seed file's prompt is put back whatever happens, so a failed run doesn't
// leave mgr-plan-01 changed for the next one.
const seedPrompt = (
  JSON.parse(readFileSync("../seed/delivery-hero-seed.json", "utf8")) as {
    tasks: { key: string; prompt: string }[];
  }
).tasks.find((task) => task.key === "mgr-plan-01")?.prompt;

test("AC-US53-01 E2E-04 step 3: admins A and B open mgr-plan-01, A saves, then B's save is refused and A's change stays", async ({
  browser,
}) => {
  expect(seedPrompt).toBeDefined();
  const contextA = await browser.newContext();
  const contextB = await browser.newContext();
  const pageA = await contextA.newPage();
  const pageB = await contextB.newPage();
  let changed = false;
  try {
    for (const page of [pageA, pageB]) {
      await loginAsAdmin(page);
      await page.goto("/admin/tasks/");
      await page.getByRole("link", { name: "mgr-plan-01", exact: true }).click();
      await expect(page.getByLabel(text.prompt)).toHaveValue(seedPrompt ?? "");
    }

    await pageA.getByLabel(text.prompt).fill(`A's change ${run}`);
    changed = true;
    await pageA.getByRole("button", { name: text.save }).click();
    await expect(pageA.getByText(text.saved, { exact: true })).toBeVisible();

    await pageB.getByLabel(text.prompt).fill(`B's change ${run}`);
    await pageB.getByRole("button", { name: text.save }).click();
    await expect(pageB.getByText(text.editConflict, { exact: true })).toBeVisible();
    await expectNoAxeViolations(pageB);

    await pageB.reload();
    await expect(pageB.getByLabel(text.prompt)).toHaveValue(`A's change ${run}`);
  } finally {
    if (changed) {
      await pageA.reload();
      await pageA.getByLabel(text.prompt).fill(seedPrompt ?? "");
      await pageA.getByRole("button", { name: text.save }).click();
      await expect(pageA.getByText(text.saved, { exact: true })).toBeVisible();
    }
    await contextA.close();
    await contextB.close();
  }
});
