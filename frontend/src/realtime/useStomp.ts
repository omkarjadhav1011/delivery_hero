// React hook over stompClient.ts: connects while mounted, subscribes to the given destinations and re-subscribes after
// every reconnect, and returns the connection status for the "Reconnecting…" banner (LLD section 6.2)
import { useEffect, useRef, useState } from "react";
import type { ConnectionStatus, Credentials, StompConnectionOptions } from "./stompClient";
import { createStompConnection } from "./stompClient";

const browserTimer = {
  setTimeout: (callback: () => void, ms: number) => setTimeout(callback, ms),
  clearTimeout: (handle: ReturnType<typeof setTimeout>) => clearTimeout(handle),
};

export interface UseStompOptions {
  /** Null until the credentials are known, for example before joining. */
  credentials: Credentials | null;
  destinations: readonly string[];
  onMessage: (destination: string, message: unknown) => void;
  onRefused?: () => void;
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
  createClient,
}: UseStompOptions): ConnectionStatus {
  // The status belongs to the connection for one set of credentials; a new one starts at "connecting"
  const [state, setState] = useState<{ key: string; status: ConnectionStatus }>({
    key: "",
    status: "connecting",
  });
  // The latest callbacks and credentials, read by the connection without rebuilding it on every render
  const latest = useRef({ credentials, onMessage, onRefused, createClient });
  useEffect(() => {
    latest.current = { credentials, onMessage, onRefused, createClient };
  });

  const key = credentialsKey(credentials);
  const destinationsKey = destinations.join("\n");
  useEffect(() => {
    const current = latest.current.credentials;
    if (key === "" || current === null) {
      return undefined;
    }
    const connection = createStompConnection({
      credentials: current,
      timer: browserTimer,
      onStatusChange: (status) => setState({ key, status }),
      onRefused: () => latest.current.onRefused?.(),
      createClient: latest.current.createClient,
    });
    const unsubscribes = destinationsKey
      .split("\n")
      .filter((destination) => destination !== "")
      .map((destination) =>
        connection.subscribe(destination, (message) =>
          latest.current.onMessage(destination, message),
        ),
      );
    connection.start();
    return () => {
      unsubscribes.forEach((unsubscribe) => unsubscribe());
      connection.stop();
    };
  }, [key, destinationsKey]);

  return state.key === key ? state.status : "connecting";
}
