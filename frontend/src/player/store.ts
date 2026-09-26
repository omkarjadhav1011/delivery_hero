import { create } from "zustand";
import type { ConnectionStatus } from "@/realtime/stompClient";
import type { GameState, JoinResponse } from "@/types/dto";
import { assertNever } from "@/types/assertNever";
import type { PlayerMessage } from "@/types/messages";

// The player app's Zustand store (LLD section 6.3). It changes only through the pure functions below, which apply a
// join result or a server message.

export type PlayerScreen =
  | "checking"
  | "notice"
  | "joinForm"
  | "restoring"
  | "lobby"
  | "practice"
  | "countdown"
  | "task"
  | "lockout"
  | "incident"
  | "done"
  | "timesUp"
  | "results"
  | "finished"
  | "removed"
  | "ended";

export interface PlayerState {
  code: string;
  token?: string;
  name?: string;
  screen: PlayerScreen;
  total: number;
  streak: number;
  connection: ConnectionStatus;
  // TODO(US-16, US-28, US-33, US-43): task, lockoutUntil, incident and results
}

export interface PlayerStore extends PlayerState {
  reset: (code: string) => void;
  joined: (result: JoinResponse) => void;
  receive: (message: PlayerMessage) => void;
  setConnection: (connection: ConnectionStatus) => void;
}

export function initialPlayerState(code: string): PlayerState {
  return { code, screen: "checking", total: 0, streak: 0, connection: "connecting" };
}

/** The screen for a game state, following the state diagram of LLD section 6.3. */
export function screenFor(state: GameState): PlayerScreen {
  switch (state) {
    case "CREATED":
    case "LOBBY":
      return "lobby";
    case "PRACTICE":
      return "practice";
    case "COUNTDOWN":
      return "countdown";
    case "LIVE":
    case "FROZEN":
      return "task";
    case "ENDED":
    case "REVEAL":
      return "timesUp";
    case "RESULTS":
      return "results";
    case "CLOSED":
      return "finished";
    case "CANCELLED":
      return "ended";
    default:
      return assertNever(state);
  }
}

/** A successful join: the lobby shows the server's final name until the first GAME_STATE arrives (FR-004). */
export function applyJoined(state: PlayerState, result: JoinResponse): PlayerState {
  return { ...state, token: result.token, name: result.name, screen: "lobby" };
}

export function applyServerMessage(state: PlayerState, message: PlayerMessage): PlayerState {
  switch (message.type) {
    case "GAME_STATE":
      return {
        ...state,
        name: message.you.name,
        total: message.you.total,
        streak: message.you.streak,
        screen: screenFor(message.state),
      };
    default:
      return assertNever(message.type);
  }
}

export const usePlayerStore = create<PlayerStore>()((set) => ({
  ...initialPlayerState(""),
  reset: (code) => set(initialPlayerState(code)),
  joined: (result) => set((state) => applyJoined(state, result)),
  receive: (message) => set((state) => applyServerMessage(state, message)),
  setConnection: (connection) => set({ connection }),
}));
