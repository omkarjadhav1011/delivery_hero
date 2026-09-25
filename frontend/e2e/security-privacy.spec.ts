import { expect, test } from "./fixtures";

// E2E-07 security-privacy (document 15, section 9). This file holds the EN-08 steps; S1-02 adds the rest.

// The home, join, projector and admin pages (AC-EN08-02, TC-EN08-02)
const pages = ["/", "/join/?code=TEST", "/screen/?key=TEST", "/admin/login/", "/admin/"];

for (const path of pages) {
  test(`AC-EN08-02 every font, image and script on ${path} comes from the game's own address`, async ({
    page,
    baseURL,
  }) => {
    const origin = new URL(baseURL ?? "http://localhost:8080").origin;
    // An explicit log beside the shared outside-request blocker, which fails the test on any other site
    const assets: { type: string; url: string }[] = [];
    page.on("request", (request) => {
      const type = request.resourceType();
      if (type === "font" || type === "image" || type === "script") {
        assets.push({ type, url: request.url() });
      }
    });

    await page.goto(path);
    await page.waitForLoadState("networkidle");

    // The pixel font and the page's scripts load, so the log isn't empty by accident
    expect(assets.some((asset) => asset.type === "font")).toBe(true);
    expect(assets.some((asset) => asset.type === "script")).toBe(true);
    for (const asset of assets) {
      expect(new URL(asset.url).origin, `${asset.type} ${asset.url}`).toBe(origin);
    }
  });
}
