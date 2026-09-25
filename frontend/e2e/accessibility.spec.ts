import type { Page } from "@playwright/test";
import { expect, expectNoAxeViolations, test } from "./fixtures";

// E2E-08 accessibility (document 15, section 9). This file holds the EN-08 steps; S2-24 adds the rest.

// Every phone page that exists so far (AC-EN08-03, TC-EN08-03)
const phonePages = ["/", "/join/?code=TEST"];

const narrowPhone = { width: 320, height: 640 };

async function expectNoSidewaysScroll(page: Page) {
  const { scrollWidth, clientWidth } = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth,
  }));
  expect(scrollWidth, "page width at most the viewport").toBeLessThanOrEqual(clientWidth);
}

async function expectControlsReachable(page: Page) {
  const controls = page.locator("button, a[href], input, select, textarea, [tabindex='0']");
  for (const control of await controls.all()) {
    await control.scrollIntoViewIfNeeded();
    const box = await control.boundingBox();
    expect(box, "control is rendered").not.toBeNull();
    if (box) {
      expect(box.x).toBeGreaterThanOrEqual(0);
      expect(box.x + box.width).toBeLessThanOrEqual(narrowPhone.width);
    }
  }
}

test.describe("narrow phones", () => {
  test.use({ viewport: narrowPhone });

  for (const path of phonePages) {
    test(`AC-EN08-03 ${path} at 320 px has no sideways scroll and every control is reachable`, async ({
      page,
    }) => {
      await page.goto(path);
      await expectNoSidewaysScroll(page);
      await expectControlsReachable(page);
      await expectNoAxeViolations(page);
    });

    test(`AC-EN08-03 ${path} at 320 px and 200% text has no sideways scroll`, async ({ page }) => {
      await page.goto(path);
      // The phone's text-size setting scales the root font; every size is in rem (NFR-30)
      await page.evaluate(() => {
        document.documentElement.style.fontSize = "200%";
      });
      await expectNoSidewaysScroll(page);
      await expectControlsReachable(page);
    });
  }
});
