import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { copy } from "@/copy";
import type { GameView, RunPlanSummary } from "@/types/dto";
import { NewGameScreen } from "./NewGameScreen";

const text = copy.admin.newGameScreen;

vi.mock("next/navigation", () => ({
  usePathname: () => "/admin/games/",
  useRouter: () => ({ replace: vi.fn() }),
}));

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

const LOGGED_IN = { authenticated: true, expiresAt: "2026-09-30T22:00:00Z" };

const DEFAULT_PLAN: RunPlanSummary = {
  id: "8b1f6c2e-3d4a-4e5f-9a0b-1c2d3e4f5a6b",
  key: "default-5min",
  name: "Default 5-minute plan",
  roundLengthMinutes: 5,
  scoredTaskCount: 68,
  errorCount: 0,
  warningCount: 0,
  version: 0,
};

const BROKEN_PLAN: RunPlanSummary = {
  ...DEFAULT_PLAN,
  id: "9c2a7d3f-4e5b-4f6a-8b1c-2d3e4f5a6b7c",
  key: "friday-fun",
  name: "Friday fun",
  roundLengthMinutes: 4,
  scoredTaskCount: 22,
  errorCount: 1,
};

const GAME: GameView = {
  id: "3f6c1a52-8d1e-4f1b-9a3c-2b7e5d9c0a11",
  code: "K7PQ2M",
  state: "CREATED",
  test: false,
  runPlanName: "Default 5-minute plan",
  roundLengthMinutes: 5,
  joinUrl: "https://hero.example.org/join?code=K7PQ2M",
  projectorUrl: "https://hero.example.org/screen?key=Zp4Tq8Lm2Vx6Nc9Rb3Hk7w",
  createdAt: "2026-10-21T09:10:00Z",
  liveDetailsAvailable: true,
  allowedActions: ["OPEN_LOBBY", "CANCEL"],
};

function urlOf(input: RequestInfo | URL): string {
  if (typeof input === "string") {
    return input;
  }
  return input instanceof URL ? input.href : input.url;
}

type Answers = {
  current: () => Response;
  create?: (body: unknown) => Response;
};

describe("NewGameScreen", () => {
  const fetchMock = vi.fn<typeof fetch>();
  const created: unknown[] = [];

  function serve(plans: RunPlanSummary[], answers: Answers) {
    fetchMock.mockImplementation((input, init) => {
      const url = urlOf(input);
      if (url === "/api/admin/session") {
        return Promise.resolve(jsonResponse(200, LOGGED_IN));
      }
      if (url === "/api/admin/run-plans") {
        return Promise.resolve(jsonResponse(200, plans));
      }
      if (url === "/api/admin/games/current") {
        return Promise.resolve(answers.current());
      }
      if (url === "/api/admin/games" && init?.method === "POST") {
        const body: unknown = typeof init.body === "string" ? JSON.parse(init.body) : null;
        created.push(body);
        return Promise.resolve(answers.create?.(body) ?? jsonResponse(500, null));
      }
      return Promise.resolve(jsonResponse(404, null));
    });
  }

  const noGame = () => new Response(null, { status: 204 });

  beforeEach(() => {
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    fetchMock.mockReset();
    created.length = 0;
    vi.unstubAllGlobals();
  });

  it("AC-US59-01 links: creating a game shows its code, join URL, QR code and projector URL", async () => {
    serve([DEFAULT_PLAN, BROKEN_PLAN], {
      current: noGame,
      create: () => jsonResponse(201, GAME),
    });
    render(<NewGameScreen />);

    expect(await screen.findByText(text.ready)).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: text.create }));

    expect(await screen.findByText("K7PQ2M")).toBeTruthy();
    expect(created).toEqual([{ runPlanId: DEFAULT_PLAN.id }]);
    expect(screen.getByText(GAME.joinUrl)).toBeTruthy();
    expect(screen.getByRole("img", { name: text.qrLabel("K7PQ2M") })).toBeTruthy();
    const projector = screen.getByRole("link", { name: text.openProjector });
    expect(projector.getAttribute("href")).toBe(GAME.projectorUrl);
    expect(projector.getAttribute("target")).toBe("_blank");
    expect(projector.getAttribute("rel")).toBe("noopener noreferrer");
    expect(screen.queryByRole("button", { name: text.create })).toBeNull();
  });

  it("AC-US59-02 broken plan: a plan with errors shows them counted and disables Create game", async () => {
    serve([DEFAULT_PLAN, BROKEN_PLAN], { current: noGame });
    render(<NewGameScreen />);
    await screen.findByText(text.ready);

    fireEvent.change(screen.getByLabelText(text.runPlan), { target: { value: BROKEN_PLAN.id } });

    expect(await screen.findByText(text.errors(1))).toBeTruthy();
    const create = screen.getByRole("button", { name: text.create });
    expect(create.hasAttribute("disabled")).toBe(true);
  });

  it("AC-US59-02 broken plan: a refused creation lists the reasons from the server", async () => {
    serve([DEFAULT_PLAN], {
      current: noGame,
      create: () =>
        jsonResponse(422, {
          type: "about:blank",
          title: "Validation failed",
          status: 422,
          code: "VALIDATION_FAILED",
          errors: [
            {
              path: "phases.TESTING",
              code: "EMPTY_PHASE",
              message: "The Testing phase has no tasks.",
            },
          ],
        }),
    });
    render(<NewGameScreen />);
    await screen.findByText(text.ready);

    fireEvent.click(screen.getByRole("button", { name: text.create }));

    expect(await screen.findByText("The Testing phase has no tasks.")).toBeTruthy();
    expect(screen.getByText(text.planErrors)).toBeTruthy();
  });

  it("changing the plan clears an earlier refusal", async () => {
    serve([DEFAULT_PLAN, { ...BROKEN_PLAN, errorCount: 0 }], {
      current: noGame,
      create: () =>
        jsonResponse(404, {
          type: "about:blank",
          title: "Not found",
          status: 404,
          code: "NOT_FOUND",
          errors: [],
        }),
    });
    render(<NewGameScreen />);
    await screen.findByText(text.ready);

    fireEvent.click(screen.getByRole("button", { name: text.create }));
    expect(await screen.findByText(text.planGone)).toBeTruthy();

    fireEvent.change(screen.getByLabelText(text.runPlan), { target: { value: BROKEN_PLAN.id } });
    expect(screen.queryByText(text.planGone)).toBeNull();
  });

  it("AC-US59-03 one at a time: a refused creation says so and shows the open game", async () => {
    let open = false;
    serve([DEFAULT_PLAN], {
      current: () => (open ? jsonResponse(200, GAME) : noGame()),
      create: () => {
        open = true;
        return jsonResponse(409, {
          type: "about:blank",
          title: "Another game open",
          status: 409,
          code: "ANOTHER_GAME_OPEN",
          detail: "Another game is still open. Close or cancel it first.",
          errors: [],
        });
      },
    });
    render(<NewGameScreen />);
    await screen.findByText(text.ready);

    fireEvent.click(screen.getByRole("button", { name: text.create }));

    expect(await screen.findByText(text.anotherGameOpen)).toBeTruthy();
    expect(await screen.findByText("K7PQ2M")).toBeTruthy();
    expect(screen.getByRole("link", { name: text.openProjector })).toBeTruthy();
  });

  it("shows the open game instead of the form when one is open", async () => {
    serve([DEFAULT_PLAN], { current: () => jsonResponse(200, GAME) });
    render(<NewGameScreen />);

    expect(await screen.findByText("K7PQ2M")).toBeTruthy();
    expect(screen.queryByRole("button", { name: text.create })).toBeNull();
  });

  it("says when there are no run plans, with Create game disabled", async () => {
    serve([], { current: noGame });
    render(<NewGameScreen />);

    expect(await screen.findByText(text.noPlans)).toBeTruthy();
    expect(screen.getByRole("button", { name: text.create }).hasAttribute("disabled")).toBe(true);
  });

  it("says when loading fails", async () => {
    fetchMock.mockImplementation((input) =>
      Promise.resolve(
        urlOf(input) === "/api/admin/session"
          ? jsonResponse(200, LOGGED_IN)
          : jsonResponse(500, null),
      ),
    );
    render(<NewGameScreen />);

    await waitFor(() => expect(screen.getByText(text.failed)).toBeTruthy());
  });
});
