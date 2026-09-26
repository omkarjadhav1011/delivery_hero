import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { ConnectionStatus, NetworkEvents, StompConfig, StompLike } from "./stompClient";
import { brokerUrl, createStompConnection } from "./stompClient";

/** A stand-in for the @stomp/stompjs adapter that records what the wrapper does with it. */
class FakeClient implements StompLike {
  config: StompConfig | undefined;
  activations = 0;
  deactivations: (boolean | undefined)[] = [];
  subscriptions: { destination: string; onBody: (body: string) => void }[] = [];
  /** Set to hold deactivate() open until the test resolves it. */
  pendingDeactivate: (() => void) | undefined;
  holdDeactivate = false;

  get connectHeaders() {
    return this.config?.connectHeaders;
  }

  configure(config: StompConfig) {
    this.config = config;
  }

  activate() {
    this.activations += 1;
  }

  deactivate(force?: boolean) {
    this.deactivations.push(force);
    if (this.holdDeactivate) {
      return new Promise<void>((resolve) => (this.pendingDeactivate = resolve));
    }
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

  /** The socket closed, as after a network drop or a failed attempt; the library forgets its subscriptions. */
  drop() {
    this.subscriptions = [];
    this.config?.onWebSocketClose();
  }
}

/** The browser's online and offline events, fired by the test. */
class FakeNetwork implements NetworkEvents {
  private listeners = new Map<string, Set<() => void>>();

  addEventListener(type: "online" | "offline", listener: () => void) {
    const set = this.listeners.get(type) ?? new Set();
    set.add(listener);
    this.listeners.set(type, set);
  }

  removeEventListener(type: "online" | "offline", listener: () => void) {
    this.listeners.get(type)?.delete(listener);
  }

  fire(type: "online" | "offline") {
    this.listeners.get(type)?.forEach((listener) => listener());
  }

  count() {
    return [...this.listeners.values()].reduce((sum, set) => sum + set.size, 0);
  }
}

const timer = {
  setTimeout: (fn: () => void, ms: number) => setTimeout(fn, ms),
  clearTimeout: clearTimeout,
};

function connect(
  client: FakeClient,
  extra: { onRefused?: () => void; statuses?: ConnectionStatus[] } = {},
) {
  const network = new FakeNetwork();
  const connection = createStompConnection({
    credentials: { playerToken: "tok-123" },
    timer,
    onStatusChange: (status) => extra.statuses?.push(status),
    onRefused: extra.onRefused,
    createClient: () => client,
    network,
  });
  return { connection, network };
}

describe("createStompConnection", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("sends the player token in CONNECT with 10-second heartbeats, a 3-second attempt limit and no library reconnects", () => {
    const client = new FakeClient();
    connect(client).connection.start();

    expect(client.connectHeaders).toEqual({ "player-token": "tok-123" });
    expect(client.config?.heartbeatIncoming).toBe(10_000);
    expect(client.config?.heartbeatOutgoing).toBe(10_000);
    expect(client.config?.connectionTimeout).toBe(3_000);
    expect(client.config?.reconnectDelay).toBe(0);
    expect(client.activations).toBe(1);
  });

  it("sends the projector key, and nothing for an admin session", () => {
    const projector = new FakeClient();
    const admin = new FakeClient();
    const options = { timer, onStatusChange: () => {}, network: new FakeNetwork() };

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

  it("re-subscribes after a reconnect and reports each status change", async () => {
    const client = new FakeClient();
    const statuses: ConnectionStatus[] = [];
    const received: unknown[] = [];
    const { connection } = connect(client, { statuses });
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

    expect(client.deactivations).toEqual([true, true]);
    expect(client.activations).toBe(3);
    expect(statuses).toEqual(["online", "reconnecting", "online"]);
    expect(client.subscriptions.map((s) => s.destination)).toEqual(["/user/queue/game"]);
    expect(received).toEqual([{ type: "GAME_STATE", serverTime: 1 }]);
  });

  it("shows reconnecting when the first connect fails, and retries after 0.5 s", async () => {
    const client = new FakeClient();
    const statuses: ConnectionStatus[] = [];
    connect(client, { statuses }).connection.start();

    client.drop();
    await vi.advanceTimersByTimeAsync(499);
    expect(client.activations).toBe(1);
    await vi.advanceTimersByTimeAsync(1);

    expect(statuses).toEqual(["reconnecting"]);
    expect(client.activations).toBe(2);
  });

  it("subscribes a destination added while offline once connected, and not one removed while offline", () => {
    const client = new FakeClient();
    const { connection } = connect(client);
    connection.start();
    connection.subscribe("/user/queue/game", () => {});
    const unsubscribe = connection.subscribe("/user/queue/time-sync", () => {});
    expect(client.subscriptions).toEqual([]);

    unsubscribe();
    client.open();

    expect(client.subscriptions.map((s) => s.destination)).toEqual(["/user/queue/game"]);
  });

  it("drops a malformed message and still delivers the next one", () => {
    const client = new FakeClient();
    const received: unknown[] = [];
    const { connection } = connect(client);
    connection.subscribe("/user/queue/game", (message) => received.push(message));
    connection.start();
    client.open();

    client.subscriptions[0]?.onBody("{not json");
    client.subscriptions[0]?.onBody('{"type":"GAME_STATE","serverTime":2}');

    expect(received).toEqual([{ type: "GAME_STATE", serverTime: 2 }]);
  });

  it.each(["UNAUTHORIZED", "FORBIDDEN"])(
    "stops for good with the refused status after an ERROR %s",
    async (code) => {
      const client = new FakeClient();
      const onRefused = vi.fn();
      const statuses: ConnectionStatus[] = [];
      const { connection, network } = connect(client, { onRefused, statuses });
      connection.start();

      client.error(code);
      client.drop();
      network.fire("online");
      await vi.advanceTimersByTimeAsync(10_000);

      expect(onRefused).toHaveBeenCalledOnce();
      expect(statuses).toEqual(["refused"]);
      expect(connection.status()).toBe("refused");
      expect(client.activations).toBe(1);
      expect(network.count()).toBe(0);
    },
  );

  it("drops the socket when the browser goes offline, and retries at once when it's back", async () => {
    const client = new FakeClient();
    const statuses: ConnectionStatus[] = [];
    const { connection, network } = connect(client, { statuses });
    connection.start();
    client.open();

    network.fire("offline");
    expect(client.deactivations).toEqual([true]);
    client.drop();
    await vi.advanceTimersByTimeAsync(500);
    client.drop();
    network.fire("online");
    await vi.advanceTimersByTimeAsync(0);

    // The online event skipped the 1-second wait: the third activation came at once
    expect(client.activations).toBe(3);
    expect(statuses).toEqual(["online", "reconnecting"]);
  });

  it("makes no new attempt when stopped while a restart is still deactivating", async () => {
    const client = new FakeClient();
    const { connection } = connect(client);
    connection.start();
    client.open();
    client.holdDeactivate = true;

    client.drop();
    await vi.advanceTimersByTimeAsync(500);
    connection.stop();
    client.pendingDeactivate?.();
    await vi.advanceTimersByTimeAsync(10_000);

    expect(client.activations).toBe(1);
  });

  it("unsubscribes, removes its listeners and stops for good", async () => {
    const client = new FakeClient();
    const { connection, network } = connect(client);
    const unsubscribe = connection.subscribe("/user/queue/game", () => {});
    connection.start();
    client.open();

    unsubscribe();
    connection.stop();
    client.drop();
    await vi.advanceTimersByTimeAsync(10_000);

    expect(client.subscriptions).toEqual([]);
    expect(client.deactivations).toEqual([undefined]);
    expect(client.activations).toBe(1);
    expect(network.count()).toBe(0);
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
