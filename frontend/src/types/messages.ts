import type { GameState } from "./dto";

// Real-time messages, mirroring document 11 section 8. Every server message carries type and serverTime (DEC-162).
// TODO(US-16): the rest of the player messages of section 8.5, checked against contracts/

export type Envelope = {
  type: string;
  serverTime: number;
};

/** The player's full state, on subscribe and on every state change (document 11, section 8.5). */
export type GameStateMessage = Envelope & {
  type: "GAME_STATE";
  gameId: string;
  state: GameState;
  you: {
    playerId: string;
    name: string;
    total: number;
    streak: number;
    streakBonusNext: boolean;
    done: boolean;
  };
  // TODO(US-13, US-16, US-28, US-33, US-10): the shapes of round, task, lockoutUntil, incident and practice
  round: unknown;
  task: unknown;
  lockoutUntil: number | null;
  incident: unknown;
  practice: unknown;
};

/** Every message on `/user/queue/game` so far. */
export type PlayerMessage = GameStateMessage;

const PLAYER_MESSAGE_TYPES: ReadonlySet<string> = new Set<PlayerMessage["type"]>(["GAME_STATE"]);

/** Narrows a parsed frame to a known player message; clients ignore types they don't know (AP-04). */
export function isPlayerMessage(value: unknown): value is PlayerMessage {
  return (
    typeof value === "object" &&
    value !== null &&
    typeof (value as { type?: unknown }).type === "string" &&
    PLAYER_MESSAGE_TYPES.has((value as { type: string }).type)
  );
}
