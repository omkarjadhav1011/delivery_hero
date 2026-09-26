import { create } from "zustand";
import { copy } from "@/copy";
import { assertNever } from "@/types/assertNever";
import type { GameState } from "@/types/dto";
import type { ScreenMessage, ScreenPlayer } from "@/types/messages";

// The projector's Zustand store (LLD section 6.4): the latest SCREEN_STATE plus the wall's JOINED events. It changes
// only through the pure functions below, which apply the CONNECTED headers, a server message or a refusal.
// TODO(US-39, US-40): the wall squares' states and the top 10

export type ScreenView = "connecting" | "gettingReady" | "lobby" | "round" | "ended";

export interface ScreenState {
  /** Learned from the CONNECTED frame, since the page holds only the key (DI-79). */
  gameId: string | null;
  view: ScreenView;
  joinUrl: string | null;
  /** Newest first (FR-053). */
  players: ScreenPlayer[];
  playerCount: number;
  endedMessage: string | null;
}

export interface ScreenStore extends ScreenState {
  connected: (headers: Record<string, string>) => void;
  receive: (message: ScreenMessage) => void;
  refused: () => void;
}

export function initialScreenState(): ScreenState {
  return {
    gameId: null,
    view: "connecting",
    joinUrl: null,
    players: [],
    playerCount: 0,
    endedMessage: null,
  };
}

const PROJECTOR_NAME = /^projector:([0-9a-f-]{36})$/;

/** The CONNECTED frame names the projector "projector:<game ID>" (DI-79). */
export function applyConnected(state: ScreenState, headers: Record<string, string>): ScreenState {
  const match = PROJECTOR_NAME.exec(headers["user-name"] ?? "");
  return match?.[1] === undefined ? state : { ...state, gameId: match[1] };
}

/** The game's screen topic once the game is known, or null before (document 11, section 8.2). */
export function screenTopic(state: ScreenState): string | null {
  return state.gameId === null ? null : `/topic/games/${state.gameId}/screen`;
}

function viewFor(state: GameState): ScreenView {
  switch (state) {
    case "CREATED":
      return "gettingReady";
    case "LOBBY":
      return "lobby";
    case "PRACTICE":
    case "COUNTDOWN":
    case "LIVE":
    case "FROZEN":
    case "ENDED":
    case "REVEAL":
    case "RESULTS":
      return "round";
    case "CLOSED":
    case "CANCELLED":
      return "ended";
    default:
      return assertNever(state);
  }
}

/** An ended game shows only its message: every piece of game data is dropped (AC-US37-03). */
function ended(message: string): ScreenState {
  return { ...initialScreenState(), view: "ended", endedMessage: message };
}

export function applyScreenMessage(state: ScreenState, message: ScreenMessage): ScreenState {
  if (state.view === "ended") {
    return state;
  }
  switch (message.type) {
    case "SCREEN_STATE":
      if (message.state === "CLOSED" || message.state === "CANCELLED") {
        return ended(message.state === "CLOSED" ? copy.screen.finished : copy.screen.hostEnded);
      }
      return {
        ...state,
        gameId: message.gameId,
        view: viewFor(message.state),
        joinUrl: message.joinUrl,
        players: message.players,
        playerCount: message.playerCount,
      };
    case "WALL_EVENTS": {
      const known = new Set(state.players.map((player) => player.playerId));
      const newcomers = message.events
        .filter((event) => event.event === "JOINED" && !known.has(event.playerId))
        .map(({ playerId, initials, firstName }): ScreenPlayer => ({
          playerId,
          initials,
          firstName,
          status: "ONLINE",
        }))
        .reverse();
      return {
        ...state,
        players: [...newcomers, ...state.players],
        playerCount: state.playerCount + newcomers.length,
      };
    }
    case "GAME_ENDED":
      return ended(message.reason === "FINISHED" ? copy.screen.finished : copy.screen.hostEnded);
    default:
      return assertNever(message);
  }
}

/** A wrong or revoked key: the server can't say which, so the finished message is shown (DI-76). */
export function applyRefused(): ScreenState {
  return ended(copy.screen.finished);
}

export const useScreenStore = create<ScreenStore>()((set) => ({
  ...initialScreenState(),
  connected: (headers) => set((state) => applyConnected(state, headers)),
  receive: (message) => set((state) => applyScreenMessage(state, message)),
  refused: () => set(applyRefused()),
}));
