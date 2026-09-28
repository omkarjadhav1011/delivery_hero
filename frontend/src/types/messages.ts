import type { GameState, HostAction } from "./dto";

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

/** One square on the projector's wall (document 11, section 8.6; DI-80 for the name fields and status). */
export type ScreenPlayer = {
  playerId: string;
  initials: string;
  firstName: string;
  status: "ONLINE" | "OFFLINE";
};

/** The projector's full state, on subscribe and on every state change (document 11, section 8.6). */
export type ScreenStateMessage = Envelope & {
  type: "SCREEN_STATE";
  gameId: string;
  state: GameState;
  test: boolean;
  joinUrl: string;
  players: ScreenPlayer[];
  playerCount: number;
  // TODO(US-10, US-21, US-39, US-41, US-34, US-43): the shapes of practice, round, top10, feed, incident and reveal
  practice: unknown;
  round: unknown;
  top10: unknown[];
  frozen: boolean;
  feed: unknown[];
  incident: unknown;
  reveal: unknown;
};

/** What happened to one square since the last batch (document 11, section 9.5). */
export type WallEvent = {
  playerId: string;
  // TODO(US-40): the other events of section 9.5
  event: "JOINED";
  initials: string;
  firstName: string;
  streak: number | null;
};

/** The wall's changes, every 500 ms when there are any (document 11, section 8.6). */
export type WallEventsMessage = Envelope & {
  type: "WALL_EVENTS";
  events: WallEvent[];
};

/** The game was closed or cancelled (document 11, sections 8.5 and 8.6). */
export type GameEndedMessage = Envelope & {
  type: "GAME_ENDED";
  reason: "FINISHED" | "CANCELLED";
};

/** Every message on `/topic/games/{gameId}/screen` so far. */
export type ScreenMessage = ScreenStateMessage | WallEventsMessage | GameEndedMessage;

const SCREEN_MESSAGE_TYPES: ReadonlySet<string> = new Set<ScreenMessage["type"]>([
  "SCREEN_STATE",
  "WALL_EVENTS",
  "GAME_ENDED",
]);

/** Narrows a parsed frame to a known projector message; clients ignore types they don't know (AP-04). */
export function isScreenMessage(value: unknown): value is ScreenMessage {
  return (
    typeof value === "object" &&
    value !== null &&
    typeof (value as { type?: unknown }).type === "string" &&
    SCREEN_MESSAGE_TYPES.has((value as { type: string }).type)
  );
}

/** A scored task's answers so far, on the live control screen (document 11, section 8.7). */
export type LiveTaskStats = {
  taskKey: string;
  answers: number;
  wrongPercent: number;
  voided: boolean;
};

/** The admin's live statistics, every 500 ms while the game is open (document 11, section 8.7). */
export type LiveStatsMessage = Envelope & {
  type: "LIVE_STATS";
  state: GameState;
  /** Epoch milliseconds; null before the countdown. */
  round: { startsAt: number; endsAt: number } | null;
  players: { joined: number; connected: number; done: number };
  incident: "NONE" | "PENDING" | "ACTIVE" | "DONE";
  tasks: LiveTaskStats[];
  allowedActions: HostAction[];
};

/** Every message on `/topic/games/{gameId}/admin` so far. */
export type AdminMessage = LiveStatsMessage | GameEndedMessage;

const ADMIN_MESSAGE_TYPES: ReadonlySet<string> = new Set<AdminMessage["type"]>([
  "LIVE_STATS",
  "GAME_ENDED",
]);

/** Narrows a parsed frame to a known admin message; clients ignore types they don't know (AP-04). */
export function isAdminMessage(value: unknown): value is AdminMessage {
  return (
    typeof value === "object" &&
    value !== null &&
    typeof (value as { type?: unknown }).type === "string" &&
    ADMIN_MESSAGE_TYPES.has((value as { type: string }).type)
  );
}
