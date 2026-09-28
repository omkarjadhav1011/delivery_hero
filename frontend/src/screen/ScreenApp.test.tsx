import { act, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { copy } from "@/copy";
import type { StompConfig, StompLike } from "@/realtime/stompClient";
import { ScreenApp } from "./ScreenApp";
import { initialScreenState, useScreenStore } from "./store";

const GAME = "3f6c1a52-8d1e-4f1b-9a3c-2b7e5d9c0a11";
const TOPIC = `/topic/games/${GAME}/screen`;

const search = vi.hoisted(() => ({ params: new URLSearchParams() }));
vi.mock("next/navigation", () => ({ useSearchParams: () => search.params }));

// A stand-in for the library's client. It has no way to send, as the real wrapper has none for the projector
// (DEC-140): the projector only connects, subscribes and sends time-sync requests.
function fakeClient() {
  let config: StompConfig | undefined;
  const subscribed: string[] = [];
  const published: string[] = [];
  const bodies = new Map<string, (body: string) => void>();
  const deactivate = vi.fn(() => Promise.resolve());
  const client: StompLike = {
    configure: (c) => (config = c),
    activate: vi.fn(),
    deactivate,
    subscribe: (destination, onBody) => {
      subscribed.push(destination);
      bodies.set(destination, onBody);
      return { unsubscribe: () => subscribed.splice(subscribed.indexOf(destination), 1) };
    },
    publish: (destination) => published.push(destination),
  };
  return { client, subscribed, bodies, published, deactivate, config: () => config };
}

function lobbyState() {
  return JSON.stringify({
    type: "SCREEN_STATE",
    serverTime: 1,
    gameId: GAME,
    state: "LOBBY",
    test: false,
    joinUrl: "https://hero.example.org/join?code=K7PQ2M",
    players: [{ playerId: "p1", initials: "S", firstName: "Sam", status: "ONLINE" }],
    playerCount: 1,
    practice: null,
    round: null,
    top10: [],
    frozen: false,
    feed: [],
    incident: null,
    reveal: null,
  });
}

describe("ScreenApp", () => {
  beforeEach(() => {
    useScreenStore.setState(initialScreenState());
    search.params = new URLSearchParams();
  });

  it("AC-US37-04 without a key shows the finished message and never connects", () => {
    const createClient = vi.fn(() => fakeClient().client);

    render(<ScreenApp createClient={createClient} />);

    expect(screen.getByText(copy.screen.finished)).toBeTruthy();
    expect(createClient).not.toHaveBeenCalled();
  });

  it("connects with the key, subscribes to the game the CONNECTED frame names and shows the lobby", () => {
    search.params = new URLSearchParams("key=abc");
    const fake = fakeClient();

    render(<ScreenApp createClient={() => fake.client} />);
    expect(fake.config()?.connectHeaders).toEqual({ "projector-key": "abc" });

    act(() => fake.config()?.onConnect({ "user-name": `projector:${GAME}` }));
    expect(fake.subscribed).toEqual(["/user/queue/time-sync", TOPIC]);
    // Display only: its one kind of outgoing message is a time-sync request (LD-02)
    expect(fake.published).toEqual(["/app/time-sync"]);

    act(() => fake.bodies.get(TOPIC)?.(lobbyState()));
    expect(screen.getByText(copy.screen.scanToJoin)).toBeTruthy();
    expect(screen.getByText("Sam")).toBeTruthy();
  });

  it("AC-US37-04 a refused key shows the finished message and no game data", () => {
    search.params = new URLSearchParams("key=wrong");
    const fake = fakeClient();

    render(<ScreenApp createClient={() => fake.client} />);
    act(() => fake.config()?.onStompError("UNAUTHORIZED"));

    expect(screen.getByText(copy.screen.finished)).toBeTruthy();
    expect(screen.queryByText(copy.screen.scanToJoin)).toBeNull();
  });

  it("AC-US37-03 a cancelled game shows the host ended message and disconnects", () => {
    search.params = new URLSearchParams("key=abc");
    const fake = fakeClient();

    render(<ScreenApp createClient={() => fake.client} />);
    act(() => fake.config()?.onConnect({ "user-name": `projector:${GAME}` }));
    act(() => fake.bodies.get(TOPIC)?.(lobbyState()));
    act(() =>
      fake.bodies.get(TOPIC)?.('{"type":"GAME_ENDED","serverTime":3,"reason":"CANCELLED"}'),
    );

    expect(screen.getByText(copy.screen.hostEnded)).toBeTruthy();
    expect(screen.queryByText("Sam")).toBeNull();
    expect(fake.deactivate).toHaveBeenCalled();
  });
});
