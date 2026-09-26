import type {
  Phase,
  Role,
  TaskContent,
  TaskDetail,
  TaskInput,
  TaskKind,
  TaskType,
  ValidationIssue,
} from "@/types/dto";
import { assertNever } from "@/types/assertNever";

// The task editor's form (document 12, A-04): what the admin types, and its conversion to and from the task input
// of document 11, section 7.4. The server validates every save; nothing here decides what is valid.

export type OptionField = { text: string; correct: boolean };
export type ItemField = { text: string; correctPosition: number };

export type TaskForm = {
  key: string;
  role: Role;
  kind: TaskKind;
  phase: Phase | null;
  type: TaskType;
  prompt: string;
  /** Null when the task has no code snippet. */
  codeLanguage: string | null;
  codeText: string;
  /** As typed; empty uses the type's default (DEC-74). */
  timeLimit: string;
  explanation: string;
  options: OptionField[];
  answerYes: boolean;
  items: ItemField[];
  markedText: string;
  monospace: boolean;
  /** The version last read; null for a new task. */
  version: number | null;
};

export const ROLES: readonly Role[] = ["MANAGER", "BUSINESS_ANALYST", "DEVELOPER", "TESTER"];
export const KINDS: readonly TaskKind[] = ["SCORED", "PRACTICE", "INCIDENT"];
export const PHASES: readonly Phase[] = ["PLANNING", "DEVELOPMENT", "TESTING", "RELEASE"];
export const TYPES: readonly TaskType[] = ["MULTIPLE_CHOICE", "YES_NO", "ORDER", "PROBLEM_WORDS"];
/** The code snippet's language labels (SRS section 7.3). */
export const LANGUAGES = [
  "text",
  "java",
  "javascript",
  "typescript",
  "sql",
  "json",
  "python",
  "shell",
] as const;

/** DEC-74, as the server applies it: incident tasks get 20 seconds whatever their type. */
export function defaultTimeLimit(type: TaskType, kind: TaskKind): number {
  if (kind === "INCIDENT") {
    return 20;
  }
  switch (type) {
    case "MULTIPLE_CHOICE":
      return 15;
    case "YES_NO":
      return 8;
    case "ORDER":
      return 25;
    case "PROBLEM_WORDS":
      return 20;
    default:
      return assertNever(type);
  }
}

// Blank content for a new task: the fewest options or items allowed, with a display order that isn't the correct one
const BLANK_OPTIONS: OptionField[] = [
  { text: "", correct: true },
  { text: "", correct: false },
];
const BLANK_ITEMS: ItemField[] = [
  { text: "", correctPosition: 2 },
  { text: "", correctPosition: 1 },
  { text: "", correctPosition: 3 },
];

export function emptyTaskForm(): TaskForm {
  return {
    key: "",
    role: "MANAGER",
    kind: "SCORED",
    phase: "PLANNING",
    type: "MULTIPLE_CHOICE",
    prompt: "",
    codeLanguage: null,
    codeText: "",
    timeLimit: "",
    explanation: "",
    options: BLANK_OPTIONS.map((option) => ({ ...option })),
    answerYes: true,
    items: BLANK_ITEMS.map((item) => ({ ...item })),
    markedText: "",
    monospace: false,
    version: null,
  };
}

/** The form with another type; each type keeps its own fields, so switching back loses nothing. */
export function withType(form: TaskForm, type: TaskType): TaskForm {
  return { ...form, type };
}

export function taskFormFromDetail(task: TaskDetail): TaskForm {
  const form: TaskForm = {
    ...emptyTaskForm(),
    key: task.key,
    role: task.role,
    kind: task.kind,
    phase: task.phase,
    type: task.type,
    prompt: task.prompt,
    codeLanguage: task.code?.language ?? null,
    codeText: task.code?.text ?? "",
    timeLimit: task.timeLimitSeconds === null ? "" : String(task.timeLimitSeconds),
    explanation: task.explanation ?? "",
    version: task.version,
  };
  const content = task.content;
  if ("options" in content) {
    form.options = content.options.map((option) => ({ ...option }));
  } else if ("answerYes" in content) {
    form.answerYes = content.answerYes;
  } else if ("items" in content) {
    form.items = content.items.map((item) => ({ ...item }));
  } else {
    form.markedText = content.markedText;
    form.monospace = content.monospace;
  }
  return form;
}

function contentOf(form: TaskForm): TaskContent {
  switch (form.type) {
    case "MULTIPLE_CHOICE":
      return { options: form.options.map((option) => ({ ...option })) };
    case "YES_NO":
      return { answerYes: form.answerYes };
    case "ORDER":
      return { items: form.items.map((item) => ({ ...item })) };
    case "PROBLEM_WORDS":
      return { markedText: form.markedText, monospace: form.monospace };
    default:
      return assertNever(form.type);
  }
}

export function taskInputFromForm(form: TaskForm): TaskInput {
  const limit = form.timeLimit.trim();
  const input: TaskInput = {
    key: form.key.trim(),
    role: form.role,
    kind: form.kind,
    // Only scored tasks have a phase (SRS section 7.3)
    phase: form.kind === "SCORED" ? form.phase : null,
    type: form.type,
    prompt: form.prompt,
    code: form.codeLanguage === null ? null : { language: form.codeLanguage, text: form.codeText },
    timeLimitSeconds: limit === "" ? null : Number(limit),
    content: contentOf(form),
    explanation: form.explanation.trim() === "" ? null : form.explanation,
  };
  if (form.version !== null) {
    input.version = form.version;
  }
  return input;
}

/** Issues shown beside a field, by the field's path, and the rest, shown above the Save button. */
export function splitIssues(issues: readonly ValidationIssue[]): {
  byPath: Partial<Record<string, string[]>>;
  general: ValidationIssue[];
} {
  const byPath: Partial<Record<string, string[]>> = {};
  const general: ValidationIssue[] = [];
  for (const issue of issues) {
    if (isFieldPath(issue.path)) {
      byPath[issue.path] = [...(byPath[issue.path] ?? []), issue.message];
    } else {
      general.push(issue);
    }
  }
  return { byPath, general };
}

const FIELD_PATHS = new Set([
  "key",
  "role",
  "kind",
  "phase",
  "type",
  "prompt",
  "code.language",
  "code.text",
  "timeLimitSeconds",
  "explanation",
  "content",
  "content.options",
  "content.items",
  "content.markedText",
]);

function isFieldPath(path: string): boolean {
  return FIELD_PATHS.has(path) || /^content\.(options|items)\[\d+]\.text$/.test(path);
}
