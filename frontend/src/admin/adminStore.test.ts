import { describe, expect, it } from "vitest";
import type { GameView } from "@/types/dto";
import type { LiveStatsMessage } from "@/types/messages";
import {
  adminTopic,
  applyActionResult,
  applyAdminMessage,
  applyGame,
  initialAdminState,
} from "./store";

const GAME: GameView = {
  id: "3f6c1a52-8d1e-4f1b-9a3c-2b7e5d9c0a11",
  code: "K7PQ2M",
  state: "LOBBY",
  test: false,
  runPlanName: "Quick 3-minute plan",
  roundLengthMinutes: 3,
  joinUrl: "https://hero.example.org/join?code=K7PQ2M",
  projectorUrl: "https://hero.example.org/screen?key=Zp4Tq8Lm2Vx6Nc9Rb3Hk7w",
  createdAt: "2026-10-21T09:10:00Z",
  liveDetailsAvailable: true,
  allowedActions: ["START_PRACTICE", "RENAME_PLAYER", "REMOVE_PLAYER", "CANCEL"],
};

function liveStats(overrides: Partial<LiveStatsMessage> = {}): LiveStatsMessage {
  return {
    type: "LIVE_STATS",
    serverTime: 1_792_575_205_000,
    state: "LIVE",
    round: { startsAt: 1_792_575_000_000, endsAt: 1_792_575_180_000 },
    players: { joined: 42, connected: 41, done: 3 },
    incident: "ACTIVE",
    tasks: [{ taskKey: "dev-dev-11", answers: 30, wrongPercent: 70, voided: false }],
    allowedActions: ["VOID_TASK", "CANCEL"],
    ...overrides,
  };
}

describe("admin store", () => {
  it("starts with no game and no live stats", () => {
    expect(initialAdminState()).toEqual({ game: null, stats: null });
    expect(adminTopic(initialAdminState())).toBeNull();
  });

  it("keeps the open game and subscribes to its admin topic", () => {
    const state = applyGame(initialAdminState(), GAME);

    expect(state.game).toEqual(GAME);
    expect(adminTopic(state)).toBe(`/topic/games/${GAME.id}/admin`);
  });

  it("AC-US60-05 live stats: LIVE_STATS is kept, and moves the game's state and actions along", () => {
    const stats = liveStats();

    const state = applyAdminMessage(applyGame(initialAdminState(), GAME), stats);

    expect(state.stats).toEqual(stats);
    expect(state.game?.state).toBe("LIVE");
    expect(state.game?.allowedActions).toEqual(["VOID_TASK", "CANCEL"]);
    expect(state.game?.code).toBe("K7PQ2M");
  });

  it("AC-US60-01 actions: the action response sets the state and exactly the actions valid now", () => {
    const state = applyActionResult(applyGame(initialAdminState(), GAME), {
      state: "COUNTDOWN",
      changed: true,
      allowedActions: ["CANCEL"],
    });

    expect(state.game?.state).toBe("COUNTDOWN");
    expect(state.game?.allowedActions).toEqual(["CANCEL"]);
  });

  it("GAME_ENDED leaves no open game, so the New game screen comes back", () => {
    const live = applyAdminMessage(applyGame(initialAdminState(), GAME), liveStats());

    const state = applyAdminMessage(live, {
      type: "GAME_ENDED",
      serverTime: 1_792_575_300_000,
      reason: "CANCELLED",
    });

    expect(state).toEqual(initialAdminState());
  });

  it("another game's view drops the last game's live stats", () => {
    const live = applyAdminMessage(applyGame(initialAdminState(), GAME), liveStats());

    const state = applyGame(live, { ...GAME, id: "5e1d2c3b-4a59-4687-9a0b-1c2d3e4f5a6b" });

    expect(state.stats).toBeNull();
  });

  it("a refreshed view of the same game keeps its live stats", () => {
    const live = applyAdminMessage(applyGame(initialAdminState(), GAME), liveStats());

    const state = applyGame(live, { ...GAME, state: "LIVE" });

    expect(state.stats).not.toBeNull();
  });

  it("messages without an open game change nothing", () => {
    expect(applyAdminMessage(initialAdminState(), liveStats())).toEqual(initialAdminState());
    expect(
      applyActionResult(initialAdminState(), { state: "LOBBY", changed: true, allowedActions: [] }),
    ).toEqual(initialAdminState());
  });
});
