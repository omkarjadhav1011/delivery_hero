import { act, renderHook } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { StompConfig, StompLike } from "./stompClient";
import { useStomp } from "./useStomp";

function fakeClient() {
  let config: StompConfig | undefined;
  const subscribed: string[] = [];
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
  };
  return { client, subscribed, bodies, deactivate, config: () => config };
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

    act(() => fake.config()?.onConnect());
    fake.bodies.get("/user/queue/game")?.('{"type":"GAME_STATE","serverTime":5}');

    expect(result.current).toBe("online");
    expect(fake.subscribed).toEqual(["/user/queue/game"]);
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

    act(() => fake.config()?.onConnect());

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
    act(() => fake.config()?.onConnect());

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
    act(() => fake.config()?.onConnect());

    rerender({ onMessage: second });
    fake.bodies.get("/user/queue/game")?.('{"type":"GAME_STATE","serverTime":6}');

    expect(createClient).toHaveBeenCalledOnce();
    expect(first).not.toHaveBeenCalled();
    expect(second).toHaveBeenCalledWith("/user/queue/game", { type: "GAME_STATE", serverTime: 6 });
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
    act(() => fakes[0]?.config()?.onConnect());
    expect(result.current).toBe("online");

    rerender({ token: "tok-2" });

    expect(result.current).toBe("connecting");
    expect(fakes[1]?.config()?.connectHeaders).toEqual({ "player-token": "tok-2" });
  });
});
