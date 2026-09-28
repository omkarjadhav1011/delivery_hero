import { describe, expect, it } from "vitest";
import { copy } from "@/copy";
import type { ScreenStateMessage, WallEventsMessage } from "@/types/messages";
import {
  applyConnected,
  applyRefused,
  applyScreenMessage,
  initialScreenState,
  screenTopic,
} from "./store";

const GAME = "3f6c1a52-8d1e-4f1b-9a3c-2b7e5d9c0a11";

function screenState(overrides: Partial<ScreenStateMessage> = {}): ScreenStateMessage {
  return {
    type: "SCREEN_STATE",
    serverTime: 1,
    gameId: GAME,
    state: "LOBBY",
    test: false,
    joinUrl: "https://hero.example.org/join?code=K7PQ2M",
    players: [],
    playerCount: 0,
    practice: null,
    round: null,
    top10: [],
    frozen: false,
    feed: [],
    incident: null,
    reveal: null,
    ...overrides,
  };
}

function joined(...names: [string, string][]): WallEventsMessage {
  return {
    type: "WALL_EVENTS",
    serverTime: 2,
    events: names.map(([playerId, firstName]) => ({
      playerId,
      event: "JOINED",
      initials: firstName.slice(0, 1),
      firstName,
      streak: null,
    })),
  };
}

describe("screen store", () => {
  it("learns its game from the CONNECTED frame's user-name, and subscribes to that game's screen", () => {
    const state = applyConnected(initialScreenState(), { "user-name": `projector:${GAME}` });

    expect(state.gameId).toBe(GAME);
    expect(screenTopic(state)).toBe(`/topic/games/${GAME}/screen`);
    expect(screenTopic(initialScreenState())).toBeNull();
    expect(applyConnected(initialScreenState(), { "user-name": "p:someone" }).gameId).toBeNull();
  });

  it("DEC-170 Created shows Getting ready, with no join link to scan", () => {
    const state = applyScreenMessage(initialScreenState(), screenState({ state: "CREATED" }));

    expect(state.view).toBe("gettingReady");
  });

  it("AC-US38-02 Lobby lists the joined names newest first, and JOINED events add new names at the top", () => {
    let state = applyScreenMessage(
      initialScreenState(),
      screenState({
        players: [{ playerId: "p1", initials: "S", firstName: "Sam", status: "ONLINE" }],
        playerCount: 1,
      }),
    );
    expect(state.view).toBe("lobby");
    expect(state.joinUrl).toBe("https://hero.example.org/join?code=K7PQ2M");

    state = applyScreenMessage(state, joined(["p2", "Priya"], ["p3", "Priya S"], ["p1", "Sam"]));

    expect(state.players.map((player) => player.firstName)).toEqual(["Priya S", "Priya", "Sam"]);
    expect(state.playerCount).toBe(3);
  });

  it("AC-US37-03 GAME_ENDED shows the message for its reason and keeps no game data", () => {
    const lobby = applyScreenMessage(
      initialScreenState(),
      screenState({
        players: [{ playerId: "p1", initials: "S", firstName: "Sam", status: "ONLINE" }],
        playerCount: 1,
      }),
    );

    const finished = applyScreenMessage(lobby, {
      type: "GAME_ENDED",
      serverTime: 3,
      reason: "FINISHED",
    });
    const cancelled = applyScreenMessage(lobby, {
      type: "GAME_ENDED",
      serverTime: 3,
      reason: "CANCELLED",
    });

    expect(finished).toMatchObject({ view: "ended", endedMessage: copy.screen.finished });
    expect(cancelled).toMatchObject({ view: "ended", endedMessage: copy.screen.hostEnded });
    expect(finished.players).toEqual([]);
    expect(finished.joinUrl).toBeNull();
    expect(applyScreenMessage(finished, screenState()).view).toBe("ended");
  });

  it("AC-US37-03 AC-US37-04 a refused connection shows the finished message and no game data (DI-79)", () => {
    const lobby = applyScreenMessage(initialScreenState(), screenState());
    const state = applyRefused(lobby);

    expect(state).toMatchObject({
      view: "ended",
      endedMessage: copy.screen.finished,
      players: [],
      joinUrl: null,
      playerCount: 0,
    });
  });

  it("AC-US37-03 a cancelled game keeps its message when a reconnect is refused or connects", () => {
    const cancelled = applyScreenMessage(applyScreenMessage(initialScreenState(), screenState()), {
      type: "GAME_ENDED",
      serverTime: 3,
      reason: "CANCELLED",
    });

    expect(applyRefused(cancelled).endedMessage).toBe(copy.screen.hostEnded);
    expect(applyConnected(cancelled, { "user-name": `projector:${GAME}` })).toBe(cancelled);
  });

  it("lists a player repeated within one WALL_EVENTS batch once", () => {
    const state = applyScreenMessage(
      applyScreenMessage(initialScreenState(), screenState()),
      joined(["p1", "Sam"], ["p1", "Sam"]),
    );

    expect(state.players.map((player) => player.playerId)).toEqual(["p1"]);
    expect(state.playerCount).toBe(1);
  });
});

describe("screen store round", () => {
  const round = {
    startsAt: 1_792_575_005_000,
    endsAt: 1_792_575_305_000,
    phases: [
      { phase: "PLANNING", startsAt: 1_792_575_005_000 },
      { phase: "DEVELOPMENT", startsAt: 1_792_575_065_000 },
      { phase: "TESTING", startsAt: 1_792_575_185_000 },
      { phase: "RELEASE", startsAt: 1_792_575_245_000 },
    ],
    releaseAt: 1_792_575_245_000,
    freezeAt: 1_792_575_275_000,
  } satisfies ScreenStateMessage["round"];

  it("AC-US13-03 the countdown's SCREEN_STATE shows the countdown with the round's times", () => {
    const state = applyScreenMessage(
      initialScreenState(),
      screenState({ state: "COUNTDOWN", round }),
    );

    expect(state.view).toBe("countdown");
    expect(state.round).toEqual(round);
  });

  it("AC-US21-01 the live and frozen round shows the live view, with the phases for the phase bar", () => {
    for (const live of ["LIVE", "FROZEN"] as const) {
      const state = applyScreenMessage(initialScreenState(), screenState({ state: live, round }));

      expect(state.view, live).toBe("live");
      expect(state.round?.phases).toHaveLength(4);
    }
  });
});
