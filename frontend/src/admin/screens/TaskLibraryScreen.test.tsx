import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { copy } from "@/copy";
import type { TaskSummary } from "@/types/dto";
import { TaskLibraryScreen } from "./TaskLibraryScreen";

const text = copy.admin.taskLibrary;
const editor = copy.admin.taskEditor;

vi.mock("next/navigation", () => ({
  usePathname: () => "/admin/tasks/",
  useRouter: () => ({ replace: vi.fn() }),
}));

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

const LOGGED_IN = { authenticated: true, expiresAt: "2026-09-30T22:00:00Z" };

const STANDUP: TaskSummary = {
  id: "0c9e7a44-2f5b-4d1e-8a3f-6b7c8d9e0f12",
  key: "mgr-dev-04",
  role: "MANAGER",
  kind: "SCORED",
  phase: "DEVELOPMENT",
  type: "MULTIPLE_CHOICE",
  prompt: "The daily standup keeps running for 40 minutes. What's the best fix?",
  effectiveTimeLimitSeconds: 15,
  usedByCount: 1,
  version: 0,
};

const INCIDENT: TaskSummary = {
  ...STANDUP,
  id: "5d2b7a44-2f5b-4d1e-8a3f-6b7c8d9e0f99",
  key: "incident-001",
  kind: "INCIDENT",
  phase: null,
  prompt: "Checkout is down!",
  effectiveTimeLimitSeconds: 20,
  usedByCount: 2,
};

function urlOf(input: RequestInfo | URL): string {
  if (typeof input === "string") {
    return input;
  }
  return input instanceof URL ? input.href : input.url;
}

describe("TaskLibraryScreen", () => {
  const fetchMock = vi.fn<typeof fetch>();
  const listed: string[] = [];

  /** Answers the session check, and each library request with the rows the answer function gives. */
  function serve(answer: (url: string) => Response) {
    fetchMock.mockImplementation((input) => {
      const url = urlOf(input);
      if (url === "/api/admin/session") {
        return Promise.resolve(jsonResponse(200, LOGGED_IN));
      }
      listed.push(url);
      return Promise.resolve(answer(url));
    });
  }

  beforeEach(() => {
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    fetchMock.mockReset();
    listed.length = 0;
    vi.unstubAllGlobals();
  });

  it("lists every task with its role, phase, type, plans and time, and the prompt under the row", async () => {
    serve(() => jsonResponse(200, [INCIDENT, STANDUP]));
    render(<TaskLibraryScreen />);

    const row = await screen.findByRole("row", { name: /mgr-dev-04/ });
    expect(within(row).getByText(editor.roles.MANAGER)).toBeTruthy();
    expect(within(row).getByText(editor.phases.DEVELOPMENT)).toBeTruthy();
    expect(within(row).getByText(editor.types.MULTIPLE_CHOICE)).toBeTruthy();
    expect(within(row).getByText(text.plans(1))).toBeTruthy();
    expect(within(row).getByText(text.seconds(15))).toBeTruthy();
    expect(screen.getByText(STANDUP.prompt)).toBeTruthy();
    const incident = screen.getByRole("row", { name: /incident-001/ });
    expect(within(incident).getByText(text.noPhase)).toBeTruthy();
    expect(within(incident).getByText(text.plans(2))).toBeTruthy();
    expect(listed).toEqual(["/api/admin/tasks"]);
  });

  it("AC-US52-01 filters by role and type on the server", async () => {
    serve(() => jsonResponse(200, [STANDUP]));
    render(<TaskLibraryScreen />);
    await screen.findByRole("row", { name: /mgr-dev-04/ });

    fireEvent.change(screen.getByLabelText(editor.role), { target: { value: "TESTER" } });
    fireEvent.change(screen.getByLabelText(editor.type), { target: { value: "ORDER" } });

    await waitFor(() => expect(listed).toContain("/api/admin/tasks?role=TESTER&type=ORDER"));
  });

  it("AC-US52-02 searches prompts with the search box, and says when nothing matches", async () => {
    serve((url) => jsonResponse(200, url.includes("q=") ? [] : [STANDUP]));
    render(<TaskLibraryScreen />);
    await screen.findByRole("row", { name: /mgr-dev-04/ });

    fireEvent.change(screen.getByLabelText(text.search), { target: { value: "nothing like it" } });

    expect(await screen.findByText(text.none)).toBeTruthy();
    expect(listed).toContain("/api/admin/tasks?q=nothing+like+it");
  });

  it("opens a task in the editor from its key, and a new one from New task", async () => {
    serve(() => jsonResponse(200, [STANDUP]));
    render(<TaskLibraryScreen />);

    // Outside the build, Link drops the trailing slash that next.config.ts's trailingSlash adds back
    const link = await screen.findByRole("link", { name: "mgr-dev-04" });
    expect(link.getAttribute("href")).toMatch(
      new RegExp(`^/admin/tasks/edit/?\\?id=${STANDUP.id}$`),
    );
    expect(screen.getByRole("link", { name: editor.newTask }).getAttribute("href")).toMatch(
      /^\/admin\/tasks\/edit\/?$/,
    );
  });

  it("says when the tasks didn't load", async () => {
    serve(() => jsonResponse(500, {}));
    render(<TaskLibraryScreen />);

    expect(await screen.findByText(text.failed)).toBeTruthy();
  });
});
