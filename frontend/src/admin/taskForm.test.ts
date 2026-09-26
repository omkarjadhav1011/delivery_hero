import { describe, expect, it } from "vitest";
import type { TaskDetail } from "@/types/dto";
import {
  defaultTimeLimit,
  emptyTaskForm,
  splitIssues,
  taskFormFromDetail,
  taskInputFromForm,
  withType,
} from "./taskForm";

const ORDER_DETAIL: TaskDetail = {
  id: "0c9e7a44-2f5b-4d1e-8a3f-6b7c8d9e0f12",
  key: "tst-test-01",
  role: "TESTER",
  kind: "SCORED",
  phase: "TESTING",
  type: "ORDER",
  prompt: "Order these bugs from most to least severe.",
  code: { language: "java", text: "int x;" },
  timeLimitSeconds: null,
  effectiveTimeLimitSeconds: 25,
  content: {
    items: [
      { text: "Profile photo upload is slow", correctPosition: 3 },
      { text: "Checkout fails for every user", correctPosition: 1 },
      { text: "Typo in the footer", correctPosition: 2 },
    ],
  },
  explanation: "A blocker beats a broken feature.",
  version: 3,
  usedBy: [{ id: "p1", name: "Default 5-minute plan" }],
  createdAt: "2026-09-30T10:12:00Z",
  updatedAt: "2026-09-30T10:12:00Z",
  warnings: [],
};

describe("taskForm", () => {
  it("DEC-74 default time limits: 15, 8, 25 and 20 seconds, and 20 for incident tasks", () => {
    expect(defaultTimeLimit("MULTIPLE_CHOICE", "SCORED")).toBe(15);
    expect(defaultTimeLimit("YES_NO", "PRACTICE")).toBe(8);
    expect(defaultTimeLimit("ORDER", "SCORED")).toBe(25);
    expect(defaultTimeLimit("PROBLEM_WORDS", "SCORED")).toBe(20);
    expect(defaultTimeLimit("MULTIPLE_CHOICE", "INCIDENT")).toBe(20);
  });

  it("round-trips a task: detail to form to the task input with its version", () => {
    const input = taskInputFromForm(taskFormFromDetail(ORDER_DETAIL));

    expect(input).toEqual({
      key: "tst-test-01",
      role: "TESTER",
      kind: "SCORED",
      phase: "TESTING",
      type: "ORDER",
      prompt: "Order these bugs from most to least severe.",
      code: { language: "java", text: "int x;" },
      timeLimitSeconds: null,
      content: ORDER_DETAIL.content,
      explanation: "A blocker beats a broken feature.",
      version: 3,
    });
  });

  it("a new task has no version, and sends only the content of its type", () => {
    const form = { ...withType(emptyTaskForm(), "YES_NO"), answerYes: true, timeLimit: "12" };

    const input = taskInputFromForm(form);

    expect(input.version).toBeUndefined();
    expect(input.content).toEqual({ answerYes: true });
    expect(input.timeLimitSeconds).toBe(12);
  });

  it("practice and incident tasks send no phase; an empty explanation or no code is null", () => {
    const form = { ...emptyTaskForm(), kind: "PRACTICE" as const, phase: "TESTING" as const };

    const input = taskInputFromForm(form);

    expect(input.phase).toBeNull();
    expect(input.explanation).toBeNull();
    expect(input.code).toBeNull();
  });

  it("a new form starts with the type's blank fields: 2 options, 3 items", () => {
    expect(taskInputFromForm(withType(emptyTaskForm(), "MULTIPLE_CHOICE")).content).toEqual({
      options: [
        { text: "", correct: true },
        { text: "", correct: false },
      ],
    });
    expect(taskInputFromForm(withType(emptyTaskForm(), "ORDER")).content).toEqual({
      items: [
        { text: "", correctPosition: 2 },
        { text: "", correctPosition: 1 },
        { text: "", correctPosition: 3 },
      ],
    });
  });

  it("splits issues into those beside a field and the rest, keyed by path", () => {
    const { byPath, general } = splitIssues([
      {
        path: "content.options",
        code: "EXACTLY_ONE_CORRECT",
        message: "Choose exactly one correct option.",
      },
      { path: "prompt", code: "REQUIRED", message: "This field is required." },
      { path: "body", code: "PATTERN", message: "Bad body." },
    ]);

    expect(byPath["content.options"]).toEqual(["Choose exactly one correct option."]);
    expect(byPath.prompt).toEqual(["This field is required."]);
    expect(general.map((issue) => issue.path)).toEqual(["body"]);
  });
});
