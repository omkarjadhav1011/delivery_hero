import { beforeEach, describe, expect, it, vi } from "vitest";

const library = vi.hoisted(() => ({
  created: [] as { options: unknown; configured: Record<string, unknown>[] }[],
}));

vi.mock("@stomp/stompjs", () => {
  class Versions {
    static V1_2 = "1.2";
    constructor(public versions: string[]) {}
  }
  class Client {
    record: { options: unknown; configured: Record<string, unknown>[] };
    constructor(options: unknown) {
      this.record = { options, configured: [] };
      library.created.push(this.record);
    }
    configure(config: Record<string, unknown>) {
      this.record.configured.push(config);
    }
    deactivate = vi.fn(() => Promise.resolve());
  }
  return { Client, Versions };
});

import { stompJsClient } from "./stompClient";

describe("stompJsClient", () => {
  beforeEach(() => {
    library.created.length = 0;
  });

  it("speaks STOMP 1.2 only and maps each setting onto the library's names", () => {
    const onStompError = vi.fn();
    const client = stompJsClient("ws://localhost:8080/ws");

    client.configure({
      connectHeaders: { "player-token": "tok" },
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      connectionTimeout: 3_000,
      reconnectDelay: 0,
      onConnect: () => {},
      onWebSocketClose: () => {},
      onStompError,
    });
    const [created] = library.created;
    const configured = created?.configured[0] ?? {};
    (configured.onStompError as (frame: { headers: Record<string, string> }) => void)({
      headers: { message: "UNAUTHORIZED" },
    });

    expect(created?.options).toEqual({
      brokerURL: "ws://localhost:8080/ws",
      stompVersions: { versions: ["1.2"] },
    });
    expect(configured).toMatchObject({
      connectHeaders: { "player-token": "tok" },
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      connectionTimeout: 3_000,
      reconnectDelay: 0,
      discardWebsocketOnCommFailure: true,
    });
    expect(onStompError).toHaveBeenCalledWith("UNAUTHORIZED");
  });
});
