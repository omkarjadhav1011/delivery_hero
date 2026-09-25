// Wraps @stomp/stompjs: player-token or projector-key in the CONNECT headers, 10-second heartbeats, our own reconnect
// schedule in place of the library's, and re-subscribing after every reconnect (LLD section 6.2, DEC-134).
import { Client, Versions } from "@stomp/stompjs";
import { createReconnectSchedule, type Timer } from "./reconnect";

/** "refused" is final: the server turned the credentials or a destination down, so no more attempts are made. */
export type ConnectionStatus = "connecting" | "online" | "reconnecting" | "refused";

export type Credentials = { playerToken: string } | { projectorKey: string } | { admin: true };

/** Heartbeats in both directions (SRS section 6.3). */
const HEARTBEAT_MS = 10_000;

/**
 * An attempt that hasn't connected by then is abandoned, so the schedule moves on even when the network swallows
 * packets instead of refusing them; it leaves room to resume within 5 seconds (NFR-03).
 */
const CONNECTION_TIMEOUT_MS = 3_000;

/** ERROR frame codes after which retrying can't help (API sections 8.1 and 8.2). */
const FINAL_ERRORS: ReadonlySet<string | undefined> = new Set(["UNAUTHORIZED", "FORBIDDEN"]);

/** What the wrapper sets on the library's client. */
export interface StompConfig {
  connectHeaders: Record<string, string>;
  heartbeatIncoming: number;
  heartbeatOutgoing: number;
  connectionTimeout: number;
  reconnectDelay: number;
  onConnect: () => void;
  onWebSocketClose: () => void;
  onStompError: (message: string | undefined) => void;
}

/** The small part of the library's client the wrapper uses, so tests can stand in for it. */
export interface StompLike {
  configure(config: StompConfig): void;
  activate(): void;
  /** With force, the socket is dropped at once instead of waiting for a closing handshake on a dead link. */
  deactivate(force?: boolean): Promise<void>;
  subscribe(destination: string, onBody: (body: string) => void): { unsubscribe(): void };
}

/** The browser's online and offline events; window in the browser. */
export interface NetworkEvents {
  addEventListener(type: "online" | "offline", listener: () => void): void;
  removeEventListener(type: "online" | "offline", listener: () => void): void;
}

export interface StompConnectionOptions {
  credentials: Credentials;
  timer: Timer;
  onStatusChange: (status: ConnectionStatus) => void;
  /** The server refused the credentials or a destination; no more attempts are made. */
  onRefused?: () => void;
  createClient?: () => StompLike;
  network?: NetworkEvents;
}

export interface StompConnection {
  start(): void;
  stop(): void;
  status(): ConnectionStatus;
  /** Delivers each parsed message on the destination, again after every reconnect; returns the unsubscribe. */
  subscribe(destination: string, onMessage: (message: unknown) => void): () => void;
}

/** The STOMP endpoint on the page's own host: wss behind HTTPS, ws on the local stack (API section 8.1). */
export function brokerUrl(location: { protocol: string; host: string }): string {
  return `${location.protocol === "https:" ? "wss" : "ws"}://${location.host}/ws`;
}

function connectHeaders(credentials: Credentials): Record<string, string> {
  if ("playerToken" in credentials) {
    return { "player-token": credentials.playerToken };
  }
  if ("projectorKey" in credentials) {
    return { "projector-key": credentials.projectorKey };
  }
  // The admin panel's session cookie authenticates the WebSocket handshake
  return {};
}

/** The real client from @stomp/stompjs, speaking STOMP 1.2 only (API section 8.1). */
export function stompJsClient(url: string): StompLike {
  const client = new Client({ brokerURL: url, stompVersions: new Versions([Versions.V1_2]) });
  return {
    configure: (config) =>
      client.configure({
        connectHeaders: config.connectHeaders,
        heartbeatIncoming: config.heartbeatIncoming,
        heartbeatOutgoing: config.heartbeatOutgoing,
        connectionTimeout: config.connectionTimeout,
        reconnectDelay: config.reconnectDelay,
        // A lost heartbeat drops the socket at once; a dead link would otherwise delay the close event
        discardWebsocketOnCommFailure: true,
        onConnect: () => config.onConnect(),
        onWebSocketClose: () => config.onWebSocketClose(),
        onStompError: (frame) => config.onStompError(frame.headers.message),
      }),
    activate: () => client.activate(),
    deactivate: (force) => client.deactivate({ force: force ?? false }),
    subscribe: (destination, onBody) =>
      client.subscribe(destination, (message) => onBody(message.body)),
  };
}

export function createStompConnection(options: StompConnectionOptions): StompConnection {
  const { credentials, timer, onStatusChange, onRefused } = options;
  const client = (options.createClient ?? (() => stompJsClient(brokerUrl(window.location))))();
  const network = options.network ?? window;
  const handlers = new Map<string, Set<(message: unknown) => void>>();
  const active = new Map<string, { unsubscribe(): void }>();
  let online = false;
  let stopped = false;

  const schedule = createReconnectSchedule({
    timer,
    // With the library's own reconnects off, the client stays active after a close, so it's restarted
    attempt: () => void client.deactivate(true).then(() => !stopped && client.activate()),
    onStatusChange,
  });

  const listen = (destination: string) => {
    if (online && !active.has(destination)) {
      const subscription = client.subscribe(destination, (body) => {
        let message: unknown;
        try {
          message = JSON.parse(body);
        } catch {
          // A malformed message is dropped; nothing about it is logged (DEC-104)
          return;
        }
        handlers.get(destination)?.forEach((handler) => handler(message));
      });
      active.set(destination, subscription);
    }
  };

  // The browser often knows first: drop a dead socket when it goes offline, and retry at once when it's back
  const onOffline = () => {
    if (online && !stopped) {
      void client.deactivate(true);
    }
  };
  const onOnline = () => schedule.retryNow();

  const removeNetworkListeners = () => {
    network.removeEventListener("offline", onOffline);
    network.removeEventListener("online", onOnline);
  };

  client.configure({
    connectHeaders: connectHeaders(credentials),
    heartbeatIncoming: HEARTBEAT_MS,
    heartbeatOutgoing: HEARTBEAT_MS,
    connectionTimeout: CONNECTION_TIMEOUT_MS,
    reconnectDelay: 0,
    onConnect: () => {
      online = true;
      // A new connection has no subscriptions: restore every one (LLD section 6.2)
      active.clear();
      handlers.forEach((_, destination) => listen(destination));
      schedule.connected();
    },
    onWebSocketClose: () => {
      active.clear();
      if (stopped) {
        return;
      }
      if (online) {
        online = false;
        schedule.connectionLost();
      } else {
        schedule.attemptFailed();
      }
    },
    onStompError: (message) => {
      if (FINAL_ERRORS.has(message)) {
        stopped = true;
        online = false;
        schedule.refuse();
        removeNetworkListeners();
        void client.deactivate(true);
        onRefused?.();
      }
    },
  });

  return {
    start: () => {
      network.addEventListener("offline", onOffline);
      network.addEventListener("online", onOnline);
      client.activate();
    },
    stop: () => {
      stopped = true;
      schedule.stop();
      removeNetworkListeners();
      void client.deactivate();
    },
    status: () => schedule.status(),
    subscribe: (destination, onMessage) => {
      const set = handlers.get(destination) ?? new Set();
      set.add(onMessage);
      handlers.set(destination, set);
      listen(destination);
      return () => {
        set.delete(onMessage);
        if (set.size === 0) {
          handlers.delete(destination);
          active.get(destination)?.unsubscribe();
          active.delete(destination);
        }
      };
    },
  };
}
