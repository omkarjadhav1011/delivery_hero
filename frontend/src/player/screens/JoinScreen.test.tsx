import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { copy } from "@/copy";
import type { JoinResponse } from "@/types/dto";
import { JoinScreen } from "./JoinScreen";

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function problem(status: number, code: string): Response {
  return jsonResponse(status, {
    type: "about:blank",
    title: code,
    status,
    code,
    detail: null,
    errors: [],
  });
}

const LOBBY = {
  gameId: "3f6c1a52-8d1e-4f1b-9a3c-2b7e5d9c0a11",
  code: "K7PQ2M",
  state: "LOBBY",
  test: false,
  joinable: true,
  reason: null,
};

const JOINED: JoinResponse = {
  gameId: LOBBY.gameId,
  playerId: "8b2d4e61-1c7a-4f9e-b0d3-5a6c7e8f9012",
  name: "Priya S",
  token: "q3Xk9vT2bLmN8pR4sW7yZa",
};

describe("JoinScreen", () => {
  const fetchMock = vi.fn<typeof fetch>();

  beforeEach(() => {
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    fetchMock.mockReset();
    vi.unstubAllGlobals();
  });

  it("AC-US01-01 a game in Lobby asks for a name, with the hint, the privacy note and Join disabled while empty", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, LOBBY));

    render(<JoinScreen code="K7PQ2M" onJoined={vi.fn()} />);

    const field = await screen.findByRole("textbox", { name: copy.join.title });
    expect(screen.getByRole("heading", { level: 1, name: copy.join.title })).toBeTruthy();
    expect(field.getAttribute("maxlength")).toBeNull();
    expect(screen.getByText(copy.join.hint)).toBeTruthy();
    expect(screen.getByText(copy.join.privacy)).toBeTruthy();
    expect(screen.getByRole("button", { name: copy.join.submit }).hasAttribute("disabled")).toBe(
      true,
    );
    expect(fetchMock.mock.calls[0]![0]).toBe("/api/games/K7PQ2M");
  });

  it("AC-US01-03 a code with no open game shows the inactive-link message and no form", async () => {
    fetchMock.mockResolvedValueOnce(problem(404, "GAME_NOT_ACTIVE"));

    render(<JoinScreen code="ZZZZ22" onJoined={vi.fn()} />);

    expect(await screen.findByText(copy.joinMessages.GAME_NOT_ACTIVE)).toBeTruthy();
    expect(screen.queryByRole("textbox")).toBeNull();
  });

  it("AC-US01-03 a join link without a code shows the inactive-link message without asking the server", async () => {
    render(<JoinScreen code={null} onJoined={vi.fn()} />);

    expect(await screen.findByText(copy.joinMessages.GAME_NOT_ACTIVE)).toBeTruthy();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("a game that isn't joinable shows its reason's message", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse(200, { ...LOBBY, state: "FROZEN", joinable: false, reason: "JOINING_CLOSED" }),
    );

    render(<JoinScreen code="K7PQ2M" onJoined={vi.fn()} />);

    expect(await screen.findByText(copy.joinMessages.JOINING_CLOSED)).toBeTruthy();
  });

  it("AC-US02-01 joining sends the name as typed and hands over the server's final name and token", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(200, LOBBY))
      .mockResolvedValueOnce(jsonResponse(201, JOINED));
    const onJoined = vi.fn();
    render(<JoinScreen code="K7PQ2M" onJoined={onJoined} />);
    const field = await screen.findByRole("textbox", { name: copy.join.title });

    fireEvent.change(field, { target: { value: "  Priya   S " } });
    fireEvent.click(screen.getByRole("button", { name: copy.join.submit }));

    await waitFor(() => expect(onJoined).toHaveBeenCalledWith(JOINED));
    const [path, init] = fetchMock.mock.calls[1]!;
    expect(path).toBe("/api/games/K7PQ2M/players");
    expect(init?.body).toBe(JSON.stringify({ name: "  Priya   S " }));
  });

  it("AC-US02-02 an invalid name shows the naming-rules message under the field, linked to it", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(200, LOBBY))
      .mockResolvedValueOnce(problem(422, "INVALID_NAME"));
    const onJoined = vi.fn();
    render(<JoinScreen code="K7PQ2M" onJoined={onJoined} />);
    const field = await screen.findByRole("textbox", { name: copy.join.title });

    fireEvent.change(field, { target: { value: "priya@home" } });
    fireEvent.click(screen.getByRole("button", { name: copy.join.submit }));

    const message = await screen.findByText(copy.join.invalidName);
    expect(field.getAttribute("aria-invalid")).toBe("true");
    expect(field.getAttribute("aria-describedby")).toContain(message.id);
    expect(onJoined).not.toHaveBeenCalled();
  });

  it("a refusal while joining, such as the game closing, replaces the form with its message", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(200, LOBBY))
      .mockResolvedValueOnce(problem(404, "GAME_NOT_ACTIVE"));
    render(<JoinScreen code="K7PQ2M" onJoined={vi.fn()} />);
    const field = await screen.findByRole("textbox", { name: copy.join.title });

    fireEvent.change(field, { target: { value: "Priya" } });
    fireEvent.click(screen.getByRole("button", { name: copy.join.submit }));

    expect(await screen.findByText(copy.joinMessages.GAME_NOT_ACTIVE)).toBeTruthy();
    expect(screen.queryByRole("textbox")).toBeNull();
  });
});
