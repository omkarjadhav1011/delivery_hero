import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { StompConfig, StompLike } from "./stompClient";
import { brokerUrl, createStompConnection } from "./stompClient";

/** A stand-in for the @stomp/stompjs adapter that records what the wrapper does with it. */
class FakeClient implements StompLike {
  config: StompConfig | undefined;
  activations = 0;
  deactivations = 0;
  subscriptions: { destination: string; onBody: (body: string) => void }[] = [];

  get connectHeaders() {
    return this.config?.connectHeaders;
  }

  configure(config: StompConfig) {
    this.config = config;
  }

  activate() {
    this.activations += 1;
  }

  deactivate() {
    this.deactivations += 1;
    return Promise.resolve();
  }

  subscribe(destination: string, onBody: (body: string) => void) {
    const entry = { destination, onBody };
    this.subscriptions.push(entry);
    return {
      unsubscribe: () => (this.subscriptions = this.subscriptions.filter((s) => s !== entry)),
    };
  }

  /** The server accepted CONNECT. */
  open() {
    this.config?.onConnect();
  }

  /** The server sent an ERROR frame. */
  error(message: string) {
    this.config?.onStompError(message);
  }

  /** The socket closed, as after a network drop; the library forgets its subscriptions. */
  drop() {
    this.subscriptions = [];
    this.config?.onWebSocketClose();
  }
}

const timer = {
  setTimeout: (fn: () => void, ms: number) => setTimeout(fn, ms),
  clearTimeout: clearTimeout,
};

describe("createStompConnection", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("sends the player token in CONNECT with 10-second heartbeats and no library reconnects", () => {
    const client = new FakeClient();
    createStompConnection({
      credentials: { playerToken: "tok-123" },
      timer,
      onStatusChange: () => {},
      createClient: () => client,
    }).start();

    expect(client.connectHeaders).toEqual({ "player-token": "tok-123" });
    expect(client.config?.heartbeatIncoming).toBe(10_000);
    expect(client.config?.heartbeatOutgoing).toBe(10_000);
    expect(client.config?.reconnectDelay).toBe(0);
    expect(client.activations).toBe(1);
  });

  it("sends the projector key, and nothing for an admin session", () => {
    const projector = new FakeClient();
    const admin = new FakeClient();
    const options = { timer, onStatusChange: () => {} };

    createStompConnection({
      ...options,
      credentials: { projectorKey: "key-9" },
      createClient: () => projector,
    }).start();
    createStompConnection({
      ...options,
      credentials: { admin: true },
      createClient: () => admin,
    }).start();

    expect(projector.connectHeaders).toEqual({ "projector-key": "key-9" });
    expect(admin.connectHeaders).toEqual({});
  });

  it("AC-EN04-04 re-subscribes after a reconnect and reports each status change", async () => {
    const client = new FakeClient();
    const statuses: string[] = [];
    const received: unknown[] = [];
    const connection = createStompConnection({
      credentials: { playerToken: "tok-123" },
      timer,
      onStatusChange: (status) => statuses.push(status),
      createClient: () => client,
    });
    connection.subscribe("/user/queue/game", (message) => received.push(message));
    connection.start();
    client.open();
    expect(client.subscriptions.map((s) => s.destination)).toEqual(["/user/queue/game"]);

    client.drop();
    await vi.advanceTimersByTimeAsync(500);
    client.drop();
    await vi.advanceTimersByTimeAsync(1_000);
    client.open();
    client.subscriptions[0]?.onBody('{"type":"GAME_STATE","serverTime":1}');

    expect(client.deactivations).toBe(2);
    expect(client.activations).toBe(3);
    expect(statuses).toEqual(["online", "reconnecting", "online"]);
    expect(client.subscriptions.map((s) => s.destination)).toEqual(["/user/queue/game"]);
    expect(received).toEqual([{ type: "GAME_STATE", serverTime: 1 }]);
  });

  it("stops retrying when the server refuses the credentials", async () => {
    const client = new FakeClient();
    const onRefused = vi.fn();
    createStompConnection({
      credentials: { playerToken: "revoked" },
      timer,
      onStatusChange: () => {},
      onRefused,
      createClient: () => client,
    }).start();

    client.error("UNAUTHORIZED");
    client.drop();
    await vi.advanceTimersByTimeAsync(10_000);

    expect(onRefused).toHaveBeenCalledOnce();
    expect(client.activations).toBe(1);
  });

  it("unsubscribes and stops for good", async () => {
    const client = new FakeClient();
    const connection = createStompConnection({
      credentials: { playerToken: "tok-123" },
      timer,
      onStatusChange: () => {},
      createClient: () => client,
    });
    const unsubscribe = connection.subscribe("/user/queue/game", () => {});
    connection.start();
    client.open();

    unsubscribe();
    connection.stop();
    client.drop();
    await vi.advanceTimersByTimeAsync(10_000);

    expect(client.subscriptions).toEqual([]);
    expect(client.deactivations).toBe(1);
    expect(client.activations).toBe(1);
  });
});

describe("brokerUrl", () => {
  it("uses wss on an https page and ws on http, on the page's own host", () => {
    expect(brokerUrl({ protocol: "https:", host: "hero.example.org" })).toBe(
      "wss://hero.example.org/ws",
    );
    expect(brokerUrl({ protocol: "http:", host: "localhost:8080" })).toBe("ws://localhost:8080/ws");
  });
});
