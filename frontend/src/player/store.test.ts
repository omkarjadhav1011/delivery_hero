import { beforeEach, describe, expect, it } from "vitest";
import type { GameState, JoinResponse } from "@/types/dto";
import type { GameStateMessage } from "@/types/messages";
import {
  applyJoined,
  applyServerMessage,
  initialPlayerState,
  screenFor,
  usePlayerStore,
} from "./store";

const JOINED: JoinResponse = {
  gameId: "3f6c1a52-8d1e-4f1b-9a3c-2b7e5d9c0a11",
  playerId: "8b2d4e61-1c7a-4f9e-b0d3-5a6c7e8f9012",
  name: "Priya S",
  token: "q3Xk9vT2bLmN8pR4sW7yZa",
};

function gameState(state: GameState, total = 0): GameStateMessage {
  return {
    type: "GAME_STATE",
    serverTime: 1_792_575_000_000,
    gameId: JOINED.gameId,
    state,
    you: {
      playerId: JOINED.playerId,
      name: "Priya S",
      total,
      streak: 0,
      streakBonusNext: false,
      done: false,
    },
    round: null,
    task: null,
    lockoutUntil: null,
    incident: null,
    practice: null,
  };
}

describe("player store", () => {
  beforeEach(() => {
    usePlayerStore.getState().reset("K7PQ2M");
  });

  it("starts on the join form's check, with no token", () => {
    const state = initialPlayerState("K7PQ2M");

    expect(state).toMatchObject({ code: "K7PQ2M", screen: "checking", total: 0, streak: 0 });
    expect(state.token).toBeUndefined();
  });

  it("AC-US04-01 joining shows the lobby with the server's final name and keeps the token", () => {
    const state = applyJoined(initialPlayerState("K7PQ2M"), JOINED);

    expect(state).toMatchObject({ screen: "lobby", name: "Priya S", token: JOINED.token });
  });

  it("AC-US04-02 the lobby switches to practice, and to the countdown, on GAME_STATE with no user action", () => {
    const store = usePlayerStore.getState();
    store.joined(JOINED);
    expect(usePlayerStore.getState().screen).toBe("lobby");

    store.receive(gameState("PRACTICE"));
    expect(usePlayerStore.getState().screen).toBe("practice");

    store.receive(gameState("LOBBY"));
    expect(usePlayerStore.getState().screen).toBe("lobby");

    store.receive(gameState("COUNTDOWN"));
    expect(usePlayerStore.getState().screen).toBe("countdown");
  });

  it("GAME_STATE sets the name, total and streak from the server", () => {
    const state = applyServerMessage(applyJoined(initialPlayerState("K7PQ2M"), JOINED), {
      ...gameState("LOBBY", 120),
      you: { ...gameState("LOBBY").you, name: "Priya S 2", total: 120, streak: 3 },
    });

    expect(state).toMatchObject({ name: "Priya S 2", total: 120, streak: 3, screen: "lobby" });
  });

  it.each<[GameState, string]>([
    ["CREATED", "lobby"],
    ["LOBBY", "lobby"],
    ["PRACTICE", "practice"],
    ["COUNTDOWN", "countdown"],
    ["LIVE", "task"],
    ["FROZEN", "task"],
    ["ENDED", "timesUp"],
    ["REVEAL", "timesUp"],
    ["RESULTS", "results"],
    ["CLOSED", "finished"],
    ["CANCELLED", "ended"],
  ])("state %s shows the %s screen (LLD section 6.3)", (state, screen) => {
    expect(screenFor(state)).toBe(screen);
  });

  it("the connection status is kept for the banner", () => {
    usePlayerStore.getState().setConnection("reconnecting");

    expect(usePlayerStore.getState().connection).toBe("reconnecting");
  });
});
