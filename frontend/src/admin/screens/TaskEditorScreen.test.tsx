import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { copy } from "@/copy";
import type { TaskDetail } from "@/types/dto";
import { TaskEditorScreen } from "./TaskEditorScreen";

const text = copy.admin.taskEditor;
const replace = vi.fn();

vi.mock("next/navigation", () => ({
  usePathname: () => "/admin/tasks/edit/",
  useRouter: () => ({ replace }),
}));

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

const LOGGED_IN = { authenticated: true, expiresAt: "2026-09-30T22:00:00Z" };

const DETAIL: TaskDetail = {
  id: "0c9e7a44-2f5b-4d1e-8a3f-6b7c8d9e0f12",
  key: "mgr-plan-01",
  role: "MANAGER",
  kind: "SCORED",
  phase: "PLANNING",
  type: "YES_NO",
  prompt: "Ship on Friday?",
  code: null,
  timeLimitSeconds: null,
  effectiveTimeLimitSeconds: 8,
  content: { answerYes: false },
  explanation: "Not on a Friday.",
  version: 3,
  usedBy: [
    { id: "p1", name: "Default 5-minute plan" },
    { id: "p2", name: "Quick 3-minute plan" },
  ],
  createdAt: "2026-09-30T10:12:00Z",
  updatedAt: "2026-09-30T10:12:00Z",
  warnings: [],
};

function urlOf(input: RequestInfo | URL): string {
  if (typeof input === "string") {
    return input;
  }
  return input instanceof URL ? input.href : input.url;
}

/** Answers the shell's session check, then each request in turn. */
function serve(fetchMock: ReturnType<typeof vi.fn<typeof fetch>>, ...responses: Response[]) {
  fetchMock.mockImplementation((input) => {
    if (urlOf(input) === "/api/admin/session") {
      return Promise.resolve(jsonResponse(200, LOGGED_IN));
    }
    const next = responses.shift();
    return Promise.resolve(next ?? jsonResponse(500, {}));
  });
}

function requests(fetchMock: ReturnType<typeof vi.fn<typeof fetch>>) {
  return fetchMock.mock.calls
    .filter(([input]) => urlOf(input) !== "/api/admin/session")
    .map(([input, init]) => ({
      url: urlOf(input),
      method: init?.method ?? "GET",
      body: typeof init?.body === "string" ? (JSON.parse(init.body) as unknown) : undefined,
    }));
}

describe("TaskEditorScreen", () => {
  const fetchMock = vi.fn<typeof fetch>();

  beforeEach(() => {
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    fetchMock.mockReset();
    replace.mockReset();
    vi.unstubAllGlobals();
  });

  it("AC-US51-01 creates a yes/no task, shows the 8-second default, and opens it by its new ID", async () => {
    serve(fetchMock, jsonResponse(201, { ...DETAIL, key: "tst-test-99", usedBy: [] }));
    render(<TaskEditorScreen id={null} />);

    expect(screen.getByRole("heading", { level: 1, name: text.newTask })).toBeTruthy();
    fireEvent.change(screen.getByLabelText(text.key), { target: { value: "tst-test-99" } });
    fireEvent.change(screen.getByLabelText(text.type), { target: { value: "YES_NO" } });
    expect(screen.getByText(text.timeLimitHint(8))).toBeTruthy();
    fireEvent.change(screen.getByLabelText(text.prompt), { target: { value: "Ship on Friday?" } });
    fireEvent.click(screen.getByLabelText(text.no));
    fireEvent.click(screen.getByRole("button", { name: text.save }));

    await waitFor(() => expect(screen.getByText(text.saved)).toBeTruthy());
    const [create] = requests(fetchMock);
    expect(create).toMatchObject({
      url: "/api/admin/tasks",
      method: "POST",
      body: {
        key: "tst-test-99",
        type: "YES_NO",
        prompt: "Ship on Friday?",
        timeLimitSeconds: null,
        content: { answerYes: false },
      },
    });
    expect(replace).toHaveBeenCalledWith(`/admin/tasks/edit/?id=${DETAIL.id}`);
  });

  it("AC-US51-02 a refused save shows the server's message beside the field, and nothing is saved", async () => {
    serve(
      fetchMock,
      jsonResponse(422, {
        title: "Validation failed",
        status: 422,
        code: "VALIDATION_FAILED",
        detail: null,
        errors: [
          {
            path: "content.options",
            code: "EXACTLY_ONE_CORRECT",
            message: "Choose exactly one correct option.",
          },
        ],
      }),
    );
    render(<TaskEditorScreen id={null} />);

    fireEvent.click(screen.getByRole("button", { name: text.save }));

    const message = await screen.findByText("Choose exactly one correct option.");
    const options = screen.getByRole("group", { name: text.options });
    expect(options.getAttribute("aria-describedby")).toBe(message.id);
    expect(screen.queryByText(text.saved)).toBeNull();
    expect(replace).not.toHaveBeenCalled();
  });

  it("AC-US51-05 a task in use can't be deleted, and the screen names each plan", async () => {
    serve(fetchMock, jsonResponse(200, DETAIL));
    render(<TaskEditorScreen id={DETAIL.id} />);

    expect(
      await screen.findByText("Used by: Default 5-minute plan, Quick 3-minute plan"),
    ).toBeTruthy();
    expect(
      screen.getByRole("heading", { level: 1, name: `${copy.admin.editTask} mgr-plan-01` }),
    ).toBeTruthy();
    expect(screen.getByRole("button", { name: text.delete }).hasAttribute("disabled")).toBe(true);
    expect(screen.getByLabelText<HTMLInputElement>(text.key).readOnly).toBe(true);
  });

  it("AC-US51-04 deletes an unused task with its version and returns to the library", async () => {
    serve(
      fetchMock,
      jsonResponse(200, { ...DETAIL, usedBy: [] }),
      new Response(null, { status: 204 }),
    );
    render(<TaskEditorScreen id={DETAIL.id} />);

    const remove = await screen.findByRole("button", { name: text.delete });
    await waitFor(() => expect(remove.hasAttribute("disabled")).toBe(false));
    fireEvent.click(remove);

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/admin/tasks/"));
    expect(requests(fetchMock)[1]).toMatchObject({
      url: `/api/admin/tasks/${DETAIL.id}?version=3`,
      method: "DELETE",
    });
  });

  it("AC-US51-07 saves an edit with its version and lists the warnings in the side panel", async () => {
    serve(
      fetchMock,
      jsonResponse(200, DETAIL),
      jsonResponse(200, {
        ...DETAIL,
        version: 4,
        warnings: [
          {
            path: "prompt",
            code: "PROMPT_OVER_25_WORDS",
            message: "Prompts should be 25 words or fewer.",
          },
        ],
      }),
    );
    render(<TaskEditorScreen id={DETAIL.id} />);

    expect(await screen.findByText(text.noWarnings)).toBeTruthy();
    await waitFor(() =>
      expect(screen.getByLabelText<HTMLTextAreaElement>(text.prompt).value).toBe("Ship on Friday?"),
    );
    fireEvent.click(screen.getByRole("button", { name: text.save }));

    expect(await screen.findByText("Prompts should be 25 words or fewer.")).toBeTruthy();
    expect(requests(fetchMock)[1]).toMatchObject({
      url: `/api/admin/tasks/${DETAIL.id}`,
      method: "PUT",
      body: { version: 3, key: "mgr-plan-01" },
    });
  });

  it("a task that can't be loaded shows a message and no form, so Save can't create a new task", async () => {
    serve(fetchMock, jsonResponse(500, {}));
    render(<TaskEditorScreen id={DETAIL.id} />);

    expect(await screen.findByText(text.failed)).toBeTruthy();
    expect(screen.queryByRole("button", { name: text.save })).toBeNull();
  });

  it("a save that fails without field issues says so", async () => {
    serve(fetchMock, jsonResponse(500, {}));
    render(<TaskEditorScreen id={null} />);

    fireEvent.click(screen.getByRole("button", { name: text.save }));

    expect(await screen.findByText(text.failed)).toBeTruthy();
  });
});
