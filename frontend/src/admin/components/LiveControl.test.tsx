import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { copy } from "@/copy";
import { initialAdminState, useAdminStore } from "@/admin/store";
import type { StompConfig, StompLike } from "@/realtime/stompClient";
import type { GameState, GameView, HostAction } from "@/types/dto";
import type { LiveStatsMessage } from "@/types/messages";
import { LiveControl } from "./LiveControl";

const text = copy.admin.liveControl;
const NOW = 1_792_575_077_000;

const GAME: GameView = {
  id: "3f6c1a52-8d1e-4f1b-9a3c-2b7e5d9c0a11",
  code: "K7PQ2M",
  state: "LOBBY",
  test: false,
  runPlanName: "Default 5-minute plan",
  roundLengthMinutes: 5,
  joinUrl: "https://hero.example.org/join?code=K7PQ2M",
  projectorUrl: "https://hero.example.org/screen?key=Zp4Tq8Lm2Vx6Nc9Rb3Hk7w",
  createdAt: "2026-10-21T09:10:00Z",
  liveDetailsAvailable: true,
  allowedActions: ["START_PRACTICE", "START_ROUND", "RENAME_PLAYER", "REMOVE_PLAYER", "CANCEL"],
};
const TOPIC = `/topic/games/${GAME.id}/admin`;

/** SRS section 3.1's host actions per state, as the server sends them for a plan with practice and a player. */
const OFFERED: Record<GameState, HostAction[]> = {
  CREATED: ["OPEN_LOBBY", "CANCEL"],
  LOBBY: ["START_PRACTICE", "START_ROUND", "RENAME_PLAYER", "REMOVE_PLAYER", "CANCEL"],
  PRACTICE: ["END_PRACTICE", "CANCEL"],
  COUNTDOWN: ["CANCEL"],
  LIVE: ["VOID_TASK", "CANCEL"],
  FROZEN: ["VOID_TASK", "CANCEL"],
  ENDED: ["START_REVEAL", "VOID_TASK", "CANCEL"],
  REVEAL: ["NEXT_STEP", "PREVIOUS_STEP", "CANCEL"],
  RESULTS: ["CLOSE"],
  CLOSED: [],
  CANCELLED: [],
};

/** Every button the screen can show, with its action. */
const BUTTONS: [string, HostAction][] = [
  [text.openLobby, "OPEN_LOBBY"],
  [text.startPractice, "START_PRACTICE"],
  [text.endPractice, "END_PRACTICE"],
  [text.startRound, "START_ROUND"],
  [text.startReveal, "START_REVEAL"],
  [text.back, "PREVIOUS_STEP"],
  [text.next, "NEXT_STEP"],
  [text.cancelGame, "CANCEL"],
  [text.closeEvent, "CLOSE"],
];

function fakeClient() {
  let config: StompConfig | undefined;
  const bodies = new Map<string, (body: string) => void>();
  const client: StompLike = {
    configure: (c) => (config = c),
    activate: vi.fn(),
    deactivate: vi.fn(() => Promise.resolve()),
    subscribe: (destination, onBody) => {
      bodies.set(destination, onBody);
      return { unsubscribe: () => bodies.delete(destination) };
    },
  };
  return { client, bodies, config: () => config };
}

function liveStats(overrides: Partial<LiveStatsMessage> = {}): LiveStatsMessage {
  return {
    type: "LIVE_STATS",
    serverTime: NOW,
    state: "LIVE",
    round: { startsAt: NOW - 77_000, endsAt: NOW + 103_000 },
    players: { joined: 42, connected: 41, done: 3 },
    incident: "ACTIVE",
    tasks: [
      { taskKey: "tst-test-04", answers: 28, wrongPercent: 46, voided: false },
      { taskKey: "dev-dev-11", answers: 30, wrongPercent: 70, voided: false },
      { taskKey: "ba-ba-02", answers: 12, wrongPercent: 46, voided: true },
    ],
    allowedActions: ["VOID_TASK", "CANCEL"],
    ...overrides,
  };
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function showGame(game: GameView) {
  useAdminStore.setState({ ...initialAdminState(), game });
}

describe("LiveControl", () => {
  const fetchMock = vi.fn<typeof fetch>();

  beforeEach(() => {
    useAdminStore.setState(initialAdminState());
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    fetchMock.mockReset();
    vi.unstubAllGlobals();
    vi.useRealTimers();
  });

  it.each(Object.entries(OFFERED) as [GameState, HostAction[]][])(
    "AC-US60-01 actions by state: in %s only the allowed actions' buttons are enabled",
    (state, allowed) => {
      showGame({ ...GAME, state, allowedActions: allowed });

      render(<LiveControl createClient={() => fakeClient().client} />);

      for (const [label, action] of BUTTONS) {
        const shown = screen.queryByRole("button", { name: label });
        const revealButton = ["START_REVEAL", "PREVIOUS_STEP", "NEXT_STEP"].includes(action);
        if (revealButton && !["ENDED", "REVEAL", "RESULTS"].includes(state)) {
          expect(shown, `${label} hidden before Ended`).toBeNull();
          continue;
        }
        if (
          (action === "CLOSE") !== (state === "RESULTS") &&
          (action === "CLOSE" || action === "CANCEL")
        ) {
          expect(shown, `${label}: Close replaces Cancel only in Results`).toBeNull();
          continue;
        }
        expect(shown, label).not.toBeNull();
        expect((shown as HTMLButtonElement).disabled, label).toBe(!allowed.includes(action));
      }
    },
  );

  it("AC-US60-05 live stats: the state, time left, players, incident, and tasks most wrong first", () => {
    vi.useFakeTimers({ toFake: ["Date", "setInterval", "clearInterval"] });
    vi.setSystemTime(NOW);
    showGame(GAME);
    const fake = fakeClient();
    render(<LiveControl createClient={() => fake.client} />);

    act(() => fake.config()?.onConnect({}));
    act(() => fake.bodies.get(TOPIC)?.(JSON.stringify(liveStats())));

    expect(
      screen.getByRole("heading", { name: text.header("K7PQ2M", GAME.runPlanName, "LIVE") }),
    ).toBeTruthy();
    expect(screen.getByText(text.timeLeft("1:43"))).toBeTruthy();
    expect(screen.getByText(text.players(42, 41, 3))).toBeTruthy();
    expect(screen.getByText(text.incident.ACTIVE)).toBeTruthy();
    const rows = screen.getAllByRole("listitem").map((row) => row.textContent);
    expect(rows).toEqual([
      `dev-dev-11${text.answers(30)}${text.wrong(70)}`,
      `tst-test-04${text.answers(28)}${text.wrong(46)}`,
      `ba-ba-02${text.answers(12)}${text.wrong(46)}${text.voided}`,
    ]);
    expect(screen.getByRole("button", { name: text.startRound })).toHaveProperty("disabled", true);
  });

  it("subscribes to the open game's admin topic over the admin session, with no credentials in the headers", () => {
    showGame(GAME);
    const fake = fakeClient();

    render(<LiveControl createClient={() => fake.client} />);
    act(() => fake.config()?.onConnect({}));

    expect(fake.config()?.connectHeaders).toEqual({});
    expect([...fake.bodies.keys()]).toEqual([TOPIC]);
  });

  it("a game left in Results by a restart says its live details were lost (DEC-142)", () => {
    showGame({ ...GAME, state: "RESULTS", liveDetailsAvailable: false, allowedActions: ["CLOSE"] });

    render(<LiveControl createClient={() => fakeClient().client} />);

    expect(
      screen.getByRole("heading", {
        name: text.header("K7PQ2M", GAME.runPlanName, text.resultsLost),
      }),
    ).toBeTruthy();
  });

  it("Start round sends START_ROUND and redraws from the response", async () => {
    showGame(GAME);
    fetchMock.mockResolvedValue(
      jsonResponse(200, { state: "COUNTDOWN", changed: true, allowedActions: ["CANCEL"] }),
    );
    render(<LiveControl createClient={() => fakeClient().client} />);

    fireEvent.click(screen.getByRole("button", { name: text.startRound }));

    await waitFor(() =>
      expect(
        screen.getByRole("heading", { name: text.header("K7PQ2M", GAME.runPlanName, "COUNTDOWN") }),
      ).toBeTruthy(),
    );
    const [url, init] = fetchMock.mock.calls[0] ?? [];
    expect(url).toBe(`/api/admin/games/${GAME.id}/actions`);
    expect(init?.method).toBe("POST");
    expect(typeof init?.body === "string" ? JSON.parse(init.body) : null).toEqual({
      action: "START_ROUND",
    });
    const actions = screen.getByRole("region", { name: text.actions });
    expect(within(actions).getByRole("button", { name: text.startRound })).toHaveProperty(
      "disabled",
      true,
    );
  });

  it.each([
    [
      "Cancel game",
      { ...GAME, state: "LIVE", allowedActions: ["VOID_TASK", "CANCEL"] },
      text.cancelGame,
      text.confirmCancel,
      "CANCEL",
    ],
    [
      "Close event",
      { ...GAME, state: "RESULTS", allowedActions: ["CLOSE"] },
      text.closeEvent,
      text.confirmClose,
      "CLOSE",
    ],
  ] as [string, GameView, string, string, HostAction][])(
    "AC-US60-02 confirmation: %s asks first, sends nothing until confirmed, then sends confirm: true",
    async (_name, game, label, question, action) => {
      showGame(game);
      fetchMock.mockResolvedValue(
        jsonResponse(200, { state: "CANCELLED", changed: true, allowedActions: [] }),
      );
      render(<LiveControl createClient={() => fakeClient().client} />);

      fireEvent.click(screen.getByRole("button", { name: label }));
      const dialog = screen.getByRole("dialog", { name: question });
      fireEvent.click(within(dialog).getByRole("button", { name: text.keepGame }));
      expect(screen.queryByRole("dialog")).toBeNull();
      expect(fetchMock).not.toHaveBeenCalled();

      fireEvent.click(screen.getByRole("button", { name: label }));
      fireEvent.click(
        within(screen.getByRole("dialog", { name: question })).getByRole("button", { name: label }),
      );

      await waitFor(() => expect(fetchMock).toHaveBeenCalledOnce());
      const [, init] = fetchMock.mock.calls[0] ?? [];
      expect(typeof init?.body === "string" ? JSON.parse(init.body) : null).toEqual({
        action,
        confirm: true,
      });
      expect(screen.queryByRole("dialog")).toBeNull();
    },
  );

  it("AC-US60-02 confirmation: closing the dialog with Escape sends nothing", () => {
    showGame({ ...GAME, state: "LIVE", allowedActions: ["VOID_TASK", "CANCEL"] });
    render(<LiveControl createClient={() => fakeClient().client} />);

    fireEvent.click(screen.getByRole("button", { name: text.cancelGame }));
    fireEvent(screen.getByRole("dialog"), new Event("close"));

    expect(screen.queryByRole("dialog")).toBeNull();
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
