// Wraps @stomp/stompjs: player-token or projector-key in the CONNECT headers, 10-second heartbeats and the
// reconnect schedule (LLD section 6.2, DEC-134).
// TODO(EN-04): create the client and expose the connection status

export type ConnectionStatus = "connecting" | "online" | "reconnecting";

export type Credentials = { playerToken: string } | { projectorKey: string } | { admin: true };
