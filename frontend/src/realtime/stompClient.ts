// Wraps @stomp/stompjs: player-token or projector-key in the CONNECT headers, 10-second heartbeats, our own reconnect
// schedule in place of the library's, and re-subscribing after every reconnect (LLD section 6.2, DEC-134).
import { Client } from "@stomp/stompjs";
import { createReconnectSchedule, type Timer } from "./reconnect";

export type ConnectionStatus = "connecting" | "online" | "reconnecting";

export type Credentials = { playerToken: string } | { projectorKey: string } | { admin: true };

/** Heartbeats in both directions (SRS section 6.3). */
const HEARTBEAT_MS = 10_000;

/** The refusal code of an ERROR frame for unknown or revoked credentials (API section 8.1). */
const UNAUTHORIZED = "UNAUTHORIZED";

/** What the wrapper sets on the library's client. */
export interface StompConfig {
  connectHeaders: Record<string, string>;
  heartbeatIncoming: number;
  heartbeatOutgoing: number;
  reconnectDelay: number;
  onConnect: () => void;
  onWebSocketClose: () => void;
  onStompError: (message: string | undefined) => void;
}

/** The small part of the library's client the wrapper uses, so tests can stand in for it. */
export interface StompLike {
  configure(config: StompConfig): void;
  activate(): void;
  deactivate(): Promise<void>;
  subscribe(destination: string, onBody: (body: string) => void): { unsubscribe(): void };
}

export interface StompConnectionOptions {
  credentials: Credentials;
  timer: Timer;
  onStatusChange: (status: ConnectionStatus) => void;
  /** The server refused the token or key; no more attempts are made. */
  onRefused?: () => void;
  createClient?: () => StompLike;
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

/** The real client from @stomp/stompjs, for the page's own host. */
export function stompJsClient(url: string): StompLike {
  const client = new Client({ brokerURL: url });
  return {
    configure: (config) =>
      client.configure({
        connectHeaders: config.connectHeaders,
        heartbeatIncoming: config.heartbeatIncoming,
        heartbeatOutgoing: config.heartbeatOutgoing,
        reconnectDelay: config.reconnectDelay,
        onConnect: () => config.onConnect(),
        onWebSocketClose: () => config.onWebSocketClose(),
        onStompError: (frame) => config.onStompError(frame.headers.message),
      }),
    activate: () => client.activate(),
    deactivate: () => client.deactivate(),
    subscribe: (destination, onBody) =>
      client.subscribe(destination, (message) => onBody(message.body)),
  };
}

export function createStompConnection(options: StompConnectionOptions): StompConnection {
  const { credentials, timer, onStatusChange, onRefused } = options;
  const client = (options.createClient ?? (() => stompJsClient(brokerUrl(window.location))))();
  const handlers = new Map<string, Set<(message: unknown) => void>>();
  const active = new Map<string, { unsubscribe(): void }>();
  let online = false;
  let stopped = false;

  const schedule = createReconnectSchedule({
    timer,
    // With the library's own reconnects off, the client stays active after a close, so it's restarted
    attempt: () => void client.deactivate().then(() => !stopped && client.activate()),
    onStatusChange,
  });

  const listen = (destination: string) => {
    if (online && !active.has(destination)) {
      const subscription = client.subscribe(destination, (body) => {
        const message: unknown = JSON.parse(body);
        handlers.get(destination)?.forEach((handler) => handler(message));
      });
      active.set(destination, subscription);
    }
  };

  client.configure({
    connectHeaders: connectHeaders(credentials),
    heartbeatIncoming: HEARTBEAT_MS,
    heartbeatOutgoing: HEARTBEAT_MS,
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
      if (message === UNAUTHORIZED) {
        stopped = true;
        schedule.stop();
        onRefused?.();
      }
    },
  });

  return {
    start: () => client.activate(),
    stop: () => {
      stopped = true;
      schedule.stop();
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
