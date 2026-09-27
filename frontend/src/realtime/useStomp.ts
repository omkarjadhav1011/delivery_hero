// React hook over stompClient.ts: connects while mounted, subscribes to the given destinations and re-subscribes after
// every reconnect, and returns the connection status for the "Reconnecting…" banner (LLD section 6.2)
import { useEffect, useRef, useState } from "react";
import type {
  ConnectionStatus,
  Credentials,
  StompConnection,
  StompConnectionOptions,
} from "./stompClient";
import { createStompConnection } from "./stompClient";

const browserTimer = {
  setTimeout: (callback: () => void, ms: number) => setTimeout(callback, ms),
  clearTimeout: (handle: ReturnType<typeof setTimeout>) => clearTimeout(handle),
};

export interface UseStompOptions {
  /** Null until the credentials are known, for example before joining. */
  credentials: Credentials | null;
  /** May change while connected, as when the projector learns its game; the connection stays up. */
  destinations: readonly string[];
  onMessage: (destination: string, message: unknown) => void;
  onRefused?: () => void;
  /** Every accepted CONNECT, with the CONNECTED frame's headers. */
  onConnected?: (headers: Record<string, string>) => void;
  /** Every status change, for a store that keeps the status (LLD section 6.3). */
  onStatusChange?: (status: ConnectionStatus) => void;
  /** Tests pass a stand-in for the library's client. */
  createClient?: StompConnectionOptions["createClient"];
}

function credentialsKey(credentials: Credentials | null): string {
  if (credentials === null) {
    return "";
  }
  if ("playerToken" in credentials) {
    return `p:${credentials.playerToken}`;
  }
  return "projectorKey" in credentials ? `k:${credentials.projectorKey}` : "admin";
}

export function useStomp({
  credentials,
  destinations,
  onMessage,
  onRefused,
  onConnected,
  onStatusChange,
  createClient,
}: UseStompOptions): ConnectionStatus {
  // The status belongs to the connection for one set of credentials; a new one starts at "connecting"
  const [state, setState] = useState<{ key: string; status: ConnectionStatus }>({
    key: "",
    status: "connecting",
  });
  const [connection, setConnection] = useState<StompConnection | null>(null);
  // The latest callbacks and credentials, read by the connection without rebuilding it on every render
  const latest = useRef({
    credentials,
    onMessage,
    onRefused,
    onConnected,
    onStatusChange,
    createClient,
  });
  useEffect(() => {
    latest.current = {
      credentials,
      onMessage,
      onRefused,
      onConnected,
      onStatusChange,
      createClient,
    };
  });

  const key = credentialsKey(credentials);
  useEffect(() => {
    const current = latest.current.credentials;
    if (key === "" || current === null) {
      return undefined;
    }
    const created = createStompConnection({
      credentials: current,
      timer: browserTimer,
      onStatusChange: (status) => {
        setState({ key, status });
        latest.current.onStatusChange?.(status);
      },
      onRefused: () => latest.current.onRefused?.(),
      onConnected: (headers) => latest.current.onConnected?.(headers),
      createClient: latest.current.createClient,
    });
    created.start();
    setConnection(created);
    return () => {
      setConnection(null);
      created.stop();
    };
  }, [key]);

  // Subscriptions follow the destinations on the same connection; stompClient restores them after a reconnect
  const destinationsKey = destinations.join("\n");
  useEffect(() => {
    if (connection === null) {
      return undefined;
    }
    const unsubscribes = destinationsKey
      .split("\n")
      .filter((destination) => destination !== "")
      .map((destination) =>
        connection.subscribe(destination, (message) =>
          latest.current.onMessage(destination, message),
        ),
      );
    return () => unsubscribes.forEach((unsubscribe) => unsubscribe());
  }, [connection, destinationsKey]);

  return state.key === key ? state.status : "connecting";
}
