import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { copy } from "@/copy";
import type { ScreenPlayer } from "@/types/messages";
import { LobbyView } from "./LobbyView";

const JOIN_URL = "https://hero.example.org/join?code=K7PQ2M";

function player(playerId: string, firstName: string): ScreenPlayer {
  return { playerId, initials: firstName.slice(0, 1), firstName, status: "ONLINE" };
}

describe("LobbyView", () => {
  it('AC-US38-01 shows "Scan to join", the join URL as a QR code and as text, the Chrome line and "Joined: 0"', () => {
    render(<LobbyView joinUrl={JOIN_URL} players={[]} playerCount={0} />);

    expect(screen.getByText(copy.screen.scanToJoin)).toBeTruthy();
    const qr = screen.getByRole("img", { name: copy.screen.qrLabel });
    expect(qr.getAttribute("class")).toContain("size-100");
    expect(screen.getByText(JOIN_URL)).toBeTruthy();
    expect(screen.getByText(copy.screen.openInChrome)).toBeTruthy();
    expect(screen.getByText("Joined: 0")).toBeTruthy();
  });

  it('AC-US38-02 Sam joins, then Priya: the count shows 2 and "Priya" comes before "Sam"', () => {
    // The store keeps the names newest first, as SCREEN_STATE sends them
    render(
      <LobbyView
        joinUrl={JOIN_URL}
        players={[player("p2", "Priya"), player("p1", "Sam")]}
        playerCount={2}
      />,
    );

    expect(screen.getByText("Joined: 2")).toBeTruthy();
    const names = within(screen.getByRole("list")).getAllByRole("listitem");
    expect(names.map((item) => item.textContent)).toEqual(["Priya", "Sam"]);
  });
});
