import { S0_GAME_CODE, expect, openS0Game, test } from "./fixtures";
import { copy } from "../src/copy";

// E2E-09 Page weight (document 15, section 9): a new phone with an empty cache opens the join URL, and the bytes
// transferred, added up from the browser's resource timing, stay under 1 MB (NFR-05). Load time on throttled 4G is
// measured on production in OPS-14.

const ONE_MB = 1_000_000;

test("NFR-05 E2E-09: the join page transfers under 1 MB to a new phone with an empty cache", async ({
  request,
  newPhone,
}) => {
  await openS0Game(request);
  const phone = await newPhone();

  await phone.goto(`/join?code=${S0_GAME_CODE}`, { waitUntil: "networkidle" });
  await expect(phone.getByRole("textbox", { name: copy.join.title })).toBeVisible();

  const transferred = await phone.evaluate(() => {
    const navigation = performance.getEntriesByType("navigation") as PerformanceNavigationTiming[];
    const resources = performance.getEntriesByType("resource") as PerformanceResourceTiming[];
    return [...navigation, ...resources].reduce((sum, entry) => sum + entry.transferSize, 0);
  });

  expect(transferred, "the navigation reports transferred bytes").toBeGreaterThan(0);
  expect(transferred).toBeLessThan(ONE_MB);
});
