import { create } from "zustand";
import { assertNever } from "@/types/assertNever";
import type { GameView, HostActionResponse } from "@/types/dto";
import type { AdminMessage, LiveStatsMessage } from "@/types/messages";

// The admin panel's Zustand store (LLD section 6.5): the open game and its latest LIVE_STATS. It changes only through
// the pure functions below, which apply a game view, an action response or a message from the admin topic.

export interface AdminState {
  /** The open game, or null when none is open (DEC-101). */
  game: GameView | null;
  stats: LiveStatsMessage | null;
}

export interface AdminStore extends AdminState {
  setGame: (game: GameView | null) => void;
  actionApplied: (response: HostActionResponse) => void;
  receive: (message: AdminMessage) => void;
}

export function initialAdminState(): AdminState {
  return { game: null, stats: null };
}

/** The open game's admin topic, or null without one (document 11, section 8.2). */
export function adminTopic(state: AdminState): string | null {
  return state.game === null ? null : `/topic/games/${state.game.id}/admin`;
}

/** A game view from the REST API; the live stats belong to the game they came from. */
export function applyGame(state: AdminState, game: GameView | null): AdminState {
  const sameGame = game !== null && state.game?.id === game.id;
  return { game, stats: sameGame ? state.stats : null };
}

/** The state and the actions valid now, from a host action's response (document 11, section 7.8). */
export function applyActionResult(state: AdminState, response: HostActionResponse): AdminState {
  if (state.game === null) {
    return state;
  }
  if (response.state === "CANCELLED" || response.state === "CLOSED") {
    // The game is over: the New game screen comes back, as on GAME_ENDED
    return initialAdminState();
  }
  return {
    ...state,
    game: { ...state.game, state: response.state, allowedActions: response.allowedActions },
  };
}

export function applyAdminMessage(state: AdminState, message: AdminMessage): AdminState {
  if (state.game === null) {
    return state;
  }
  switch (message.type) {
    case "LIVE_STATS":
      return {
        game: { ...state.game, state: message.state, allowedActions: message.allowedActions },
        stats: message,
      };
    case "GAME_ENDED":
      return initialAdminState();
    default:
      return assertNever(message);
  }
}

export const useAdminStore = create<AdminStore>()((set) => ({
  ...initialAdminState(),
  setGame: (game) => set((state) => applyGame(state, game)),
  actionApplied: (response) => set((state) => applyActionResult(state, response)),
  receive: (message) => set((state) => applyAdminMessage(state, message)),
}));
