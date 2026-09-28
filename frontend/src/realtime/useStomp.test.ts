import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { StompConfig, StompLike } from "./stompClient";
import { getServerOffset, setServerOffset } from "@/time/timeSync";
import { useStomp } from "./useStomp";

const TIME_SYNC_QUEUE = "/user/queue/time-sync";

function fakeClient() {
  let config: StompConfig | undefined;
  const subscribed: string[] = [];
  const bodies = new Map<string, (body: string) => void>();
  const published: { destination: string; body: string }[] = [];
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
    publish: (destination, body) => published.push({ destination, body }),
  };
  return { client, subscribed, bodies, published, deactivate, config: () => config };
}

describe("useStomp", () => {
  it("connects with the credentials, reports the status and delivers parsed messages", () => {
    const fake = fakeClient();
    const onMessage = vi.fn();
    const { result } = renderHook(() =>
      useStomp({
        credentials: { playerToken: "tok-1" },
        destinations: ["/user/queue/game"],
        onMessage,
        createClient: () => fake.client,
      }),
    );
    expect(result.current).toBe("connecting");
    expect(fake.config()?.connectHeaders).toEqual({ "player-token": "tok-1" });

    act(() => fake.config()?.onConnect({}));
    fake.bodies.get("/user/queue/game")?.('{"type":"GAME_STATE","serverTime":5}');

    expect(result.current).toBe("online");
    expect(fake.subscribed).toEqual([TIME_SYNC_QUEUE, "/user/queue/game"]);
    expect(onMessage).toHaveBeenCalledWith("/user/queue/game", {
      type: "GAME_STATE",
      serverTime: 5,
    });
  });

  it("passes every status change to onStatusChange", () => {
    const fake = fakeClient();
    const onStatusChange = vi.fn();
    renderHook(() =>
      useStomp({
        credentials: { playerToken: "tok-1" },
        destinations: ["/user/queue/game"],
        onMessage: vi.fn(),
        onStatusChange,
        createClient: () => fake.client,
      }),
    );

    act(() => fake.config()?.onConnect({}));

    expect(onStatusChange).toHaveBeenLastCalledWith("online");
  });

  it("doesn't connect without credentials", () => {
    const createClient = vi.fn();
    renderHook(() =>
      useStomp({ credentials: null, destinations: [], onMessage: () => {}, createClient }),
    );

    expect(createClient).not.toHaveBeenCalled();
  });

  it("unsubscribes and disconnects on unmount", () => {
    const fake = fakeClient();
    const { unmount } = renderHook(() =>
      useStomp({
        credentials: { projectorKey: "key-1" },
        destinations: ["/topic/games/g/screen"],
        onMessage: () => {},
        createClient: () => fake.client,
      }),
    );
    act(() => fake.config()?.onConnect({}));

    unmount();

    expect(fake.subscribed).toEqual([]);
    expect(fake.deactivate).toHaveBeenCalledOnce();
  });

  it("keeps one connection across renders and calls the latest onMessage", () => {
    const fake = fakeClient();
    const createClient = vi.fn(() => fake.client);
    const first = vi.fn();
    const second = vi.fn();
    const { rerender } = renderHook(
      ({ onMessage }) =>
        useStomp({
          credentials: { playerToken: "tok-1" },
          destinations: ["/user/queue/game"],
          onMessage,
          createClient,
        }),
      { initialProps: { onMessage: first } },
    );
    act(() => fake.config()?.onConnect({}));

    rerender({ onMessage: second });
    fake.bodies.get("/user/queue/game")?.('{"type":"GAME_STATE","serverTime":6}');

    expect(createClient).toHaveBeenCalledOnce();
    expect(first).not.toHaveBeenCalled();
    expect(second).toHaveBeenCalledWith("/user/queue/game", { type: "GAME_STATE", serverTime: 6 });
  });

  it("passes the CONNECTED headers to onConnected, and subscribes a later destination on the same connection", () => {
    const fake = fakeClient();
    const createClient = vi.fn(() => fake.client);
    const onConnected = vi.fn();
    const { rerender } = renderHook(
      ({ destinations }) =>
        useStomp({
          credentials: { projectorKey: "key-1" },
          destinations,
          onMessage: () => {},
          onConnected,
          createClient,
        }),
      { initialProps: { destinations: [] as string[] } },
    );

    act(() => fake.config()?.onConnect({ "user-name": "projector:g-1" }));
    rerender({ destinations: ["/topic/games/g-1/screen"] });

    expect(onConnected).toHaveBeenCalledWith({ "user-name": "projector:g-1" });
    expect(fake.subscribed).toEqual([TIME_SYNC_QUEUE, "/topic/games/g-1/screen"]);
    expect(createClient).toHaveBeenCalledOnce();
    expect(fake.deactivate).not.toHaveBeenCalled();
  });

  it("reports connecting again for new credentials, not the old connection's status", () => {
    const fakes = [fakeClient(), fakeClient()];
    let made = 0;
    const createClient = () => (fakes[made++] ?? fakes[1]!).client;
    const { result, rerender } = renderHook(
      ({ token }) =>
        useStomp({
          credentials: { playerToken: token },
          destinations: ["/user/queue/game"],
          onMessage: () => {},
          createClient,
        }),
      { initialProps: { token: "tok-1" } },
    );
    act(() => fakes[0]?.config()?.onConnect({}));
    expect(result.current).toBe("online");

    rerender({ token: "tok-2" });

    expect(result.current).toBe("connecting");
    expect(fakes[1]?.config()?.connectHeaders).toEqual({ "player-token": "tok-2" });
  });
});

describe("useStomp time sync", () => {
  const NOW = 1_792_575_000_000;

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(NOW);
  });

  afterEach(() => {
    setServerOffset(0);
    vi.useRealTimers();
  });

  /** The server's reply to the latest request, `roundTrip` ms after it was sent, from a clock `ahead` ms ahead. */
  function reply(fake: ReturnType<typeof fakeClient>, roundTrip: number, ahead: number) {
    const last = fake.published.at(-1);
    const { clientSentAt } = JSON.parse(last?.body ?? "{}") as { clientSentAt: number };
    vi.advanceTimersByTime(roundTrip);
    const serverTime = clientSentAt + roundTrip / 2 + ahead;
    fake.bodies.get(TIME_SYNC_QUEUE)?.(
      JSON.stringify({ type: "TIME_SYNC", serverTime, clientSentAt }),
    );
  }

  it.each([
    ["a player", { playerToken: "tok-1" }],
    ["the projector", { projectorKey: "key-1" }],
    ["an admin", { admin: true }],
  ] as const)(
    "AC-US14-01 %s: the connection sends three TIME_SYNC requests after subscribing, and stores the offset",
    (_kind, credentials) => {
      const fake = fakeClient();
      renderHook(() =>
        useStomp({
          credentials,
          destinations: [],
          onMessage: () => {},
          createClient: () => fake.client,
        }),
      );

      act(() => fake.config()?.onConnect({}));
      expect(fake.subscribed).toEqual([TIME_SYNC_QUEUE]);
      expect(fake.published).toEqual([
        { destination: "/app/time-sync", body: `{"clientSentAt":${NOW}}` },
      ]);
      act(() => {
        reply(fake, 200, 45_000);
        reply(fake, 40, 45_000);
        reply(fake, 90, 45_000);
      });

      expect(fake.published).toHaveLength(3);
      expect(getServerOffset()).toBe(45_000);
    },
  );

  it("starts a new estimate after a reconnect, and none after unmount", () => {
    const fake = fakeClient();
    const onMessage = vi.fn();
    const { unmount } = renderHook(() =>
      useStomp({
        credentials: { playerToken: "tok-1" },
        destinations: [],
        onMessage,
        createClient: () => fake.client,
      }),
    );
    act(() => fake.config()?.onConnect({}));
    act(() => {
      reply(fake, 50, 0);
      reply(fake, 50, 0);
      reply(fake, 50, 0);
    });

    act(() => {
      fake.config()?.onWebSocketClose();
      vi.advanceTimersByTime(600);
      fake.config()?.onConnect({});
    });
    expect(fake.published).toHaveLength(4);
    expect(onMessage).not.toHaveBeenCalled();

    unmount();
    vi.advanceTimersByTime(120_000);
    expect(fake.published).toHaveLength(4);
  });
});
