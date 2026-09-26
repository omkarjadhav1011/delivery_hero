"use client";

import { useRouter } from "next/navigation";
import { useEffect, useId, useRef, useState, type FormEvent } from "react";
import { createTask, deleteTask, getTask, updateTask } from "@/api/endpoints";
import { ApiError } from "@/api/http";
import { copy } from "@/copy";
import { AdminShell } from "@/admin/components/AdminShell";
import { FieldErrors, fieldClass } from "@/admin/components/FieldErrors";
import { MultipleChoiceFields } from "@/admin/components/MultipleChoiceFields";
import { OrderFields } from "@/admin/components/OrderFields";
import { ProblemWordsFields } from "@/admin/components/ProblemWordsFields";
import { YesNoFields } from "@/admin/components/YesNoFields";
import {
  KINDS,
  LANGUAGES,
  PHASES,
  ROLES,
  TYPES,
  defaultTimeLimit,
  emptyTaskForm,
  splitIssues,
  taskFormFromDetail,
  taskInputFromForm,
  withType,
  type TaskForm,
} from "@/admin/taskForm";
import { assertNever } from "@/types/assertNever";
import type { Phase, Role, TaskDetail, TaskKind, TaskType, ValidationIssue } from "@/types/dto";
import { ArcadeButton } from "@/ui/ArcadeButton";

type TaskEditorScreenProps = {
  /** The task to edit, or null for a new one (`?id=`, document 12, section 9). */
  id: string | null;
};

const text = copy.admin.taskEditor;

type Outcome = { saved: boolean; errors: readonly ValidationIssue[]; message: string | null };
const NO_OUTCOME: Outcome = { saved: false, errors: [], message: null };

// A-04 Task editor (document 12, section 9; FR-069, FR-071): the fields change with the type, the server validates
// every save, errors appear beside their fields and warnings in the side panel. The phone preview arrives with T7.
export function TaskEditorScreen({ id }: TaskEditorScreenProps) {
  const router = useRouter();
  const [form, setForm] = useState<TaskForm>(emptyTaskForm);
  const [task, setTask] = useState<TaskDetail | null>(null);
  const [loading, setLoading] = useState(id !== null);
  // Set when the task named by ?id= couldn't be loaded: the form stays hidden, so Save can't create a new task
  const [loadFailed, setLoadFailed] = useState(false);
  const [busy, setBusy] = useState(false);
  const [outcome, setOutcome] = useState<Outcome>(NO_OUTCOME);
  const baseId = useId();
  // The task on screen, so opening it by its new ID after a create doesn't load it again
  const shown = useRef<string | null>(null);

  useEffect(() => {
    if (id === null || id === shown.current) {
      return undefined;
    }
    let cancelled = false;
    getTask(id).then(
      (detail) => {
        if (!cancelled) {
          shown.current = detail.id;
          setTask(detail);
          setForm(taskFormFromDetail(detail));
          setLoading(false);
        }
      },
      (error: unknown) => {
        if (!cancelled) {
          setLoading(false);
          setLoadFailed(true);
          const notFound = error instanceof ApiError && error.code === "NOT_FOUND";
          setOutcome({ ...NO_OUTCOME, message: notFound ? text.notFound : text.failed });
        }
      },
    );
    return () => {
      cancelled = true;
    };
  }, [id]);

  function change(patch: Partial<TaskForm>) {
    setForm((current) => ({ ...current, ...patch }));
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    // Cleared first, so a repeated message is announced again
    setOutcome(NO_OUTCOME);
    setBusy(true);
    try {
      const input = taskInputFromForm(form);
      const saved = task === null ? await createTask(input) : await updateTask(task.id, input);
      shown.current = saved.id;
      setTask(saved);
      setForm(taskFormFromDetail(saved));
      setOutcome({ ...NO_OUTCOME, saved: true });
      if (task === null) {
        router.replace(`/admin/tasks/edit/?id=${encodeURIComponent(saved.id)}`);
      }
    } catch (error: unknown) {
      // Field issues appear beside their fields; a conflict keeps the edits on screen (FR-073); anything else gets
      // the general message
      if (error instanceof ApiError && error.code === "EDIT_CONFLICT") {
        setOutcome({ ...NO_OUTCOME, message: text.editConflict });
      } else if (error instanceof ApiError && error.errors.length > 0) {
        setOutcome({ ...NO_OUTCOME, errors: error.errors });
      } else {
        setOutcome({ ...NO_OUTCOME, message: text.failed });
      }
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    if (task === null) {
      return;
    }
    setOutcome(NO_OUTCOME);
    setBusy(true);
    try {
      await deleteTask(task.id, task.version);
      router.replace("/admin/tasks/");
    } catch (error: unknown) {
      if (error instanceof ApiError && error.code === "TASK_IN_USE") {
        setOutcome({
          ...NO_OUTCOME,
          message: text.usedBy(error.errors.map((issue) => issue.message)),
        });
      } else if (error instanceof ApiError && error.code === "EDIT_CONFLICT") {
        setOutcome({ ...NO_OUTCOME, message: text.editConflict });
      } else {
        setOutcome({ ...NO_OUTCOME, message: text.failed });
      }
    } finally {
      setBusy(false);
    }
  }

  const { byPath, general } = splitIssues(outcome.errors);
  const ids = (field: string) => `${baseId}-${field}`;
  const errorsId = (field: string) => `${baseId}-${field}-errors`;
  const title = id === null ? text.newTask : `${copy.admin.editTask} ${task?.key ?? ""}`.trim();
  const usedBy = task?.usedBy ?? [];
  const warnings = task?.warnings ?? [];

  function select<T extends string>(
    field: string,
    label: string,
    value: T,
    values: readonly T[],
    names: Record<T, string>,
    onChange: (value: T) => void,
    disabled = false,
  ) {
    return (
      <div className="flex flex-col gap-1">
        <label htmlFor={ids(field)} className="font-semibold">
          {label}
        </label>
        <select
          id={ids(field)}
          value={value}
          disabled={disabled}
          onChange={(event) => onChange(event.target.value as T)}
          aria-describedby={errorsId(field)}
          className={fieldClass}
        >
          {values.map((option) => (
            <option key={option} value={option}>
              {names[option]}
            </option>
          ))}
        </select>
        <FieldErrors id={errorsId(field)} messages={byPath[field]} />
      </div>
    );
  }

  function contentFields(type: TaskType) {
    switch (type) {
      case "MULTIPLE_CHOICE":
        return (
          <MultipleChoiceFields
            options={form.options}
            errors={byPath}
            onChange={(options) => change({ options })}
          />
        );
      case "YES_NO":
        return (
          <YesNoFields answerYes={form.answerYes} onChange={(answerYes) => change({ answerYes })} />
        );
      case "ORDER":
        return (
          <OrderFields items={form.items} errors={byPath} onChange={(items) => change({ items })} />
        );
      case "PROBLEM_WORDS":
        return (
          <ProblemWordsFields
            markedText={form.markedText}
            monospace={form.monospace}
            errors={byPath}
            onChange={change}
          />
        );
      default:
        return assertNever(type);
    }
  }

  return (
    <AdminShell title={title}>
      {loadFailed ? (
        <p role="status" className="font-semibold">
          {outcome.message}
        </p>
      ) : null}
      {loading || loadFailed ? null : (
        <div className="flex flex-wrap gap-8">
          <form
            className="flex max-w-3xl flex-1 flex-col gap-4"
            onSubmit={(event) => void save(event)}
            noValidate
          >
            <div className="flex flex-col gap-1">
              <label htmlFor={ids("key")} className="font-semibold">
                {text.key}
              </label>
              <input
                id={ids("key")}
                value={form.key}
                // The key can't change after creation (document 11, section 7.4)
                readOnly={task !== null}
                onChange={(event) => change({ key: event.target.value })}
                aria-describedby={errorsId("key")}
                className={fieldClass}
              />
              <FieldErrors id={errorsId("key")} messages={byPath.key} />
            </div>
            <div className="flex flex-wrap gap-4">
              {select<Role>("role", text.role, form.role, ROLES, text.roles, (role) =>
                change({ role }),
              )}
              {select<TaskKind>("kind", text.kind, form.kind, KINDS, text.kinds, (kind) =>
                change({ kind }),
              )}
              {select<Phase>(
                "phase",
                text.phase,
                form.phase ?? "PLANNING",
                PHASES,
                text.phases,
                (phase) => change({ phase }),
                form.kind !== "SCORED",
              )}
            </div>
            <div className="flex flex-wrap items-start gap-4">
              {select<TaskType>("type", text.type, form.type, TYPES, text.types, (type) =>
                setForm((current) => withType(current, type)),
              )}
              <div className="flex flex-col gap-1">
                <label htmlFor={ids("timeLimitSeconds")} className="font-semibold">
                  {text.timeLimit}
                </label>
                <div className="flex items-center gap-2">
                  <input
                    id={ids("timeLimitSeconds")}
                    type="number"
                    min={5}
                    max={60}
                    value={form.timeLimit}
                    onChange={(event) => change({ timeLimit: event.target.value })}
                    aria-describedby={`${ids("timeLimitHint")} ${errorsId("timeLimitSeconds")}`}
                    className={`${fieldClass} w-24`}
                  />
                  <span id={ids("timeLimitHint")} className="text-text-muted">
                    {text.timeLimitHint(defaultTimeLimit(form.type, form.kind))}
                  </span>
                </div>
                <FieldErrors id={errorsId("timeLimitSeconds")} messages={byPath.timeLimitSeconds} />
              </div>
            </div>
            <div className="flex flex-col gap-1">
              <label htmlFor={ids("prompt")} className="font-semibold">
                {text.prompt}
              </label>
              <textarea
                id={ids("prompt")}
                rows={2}
                value={form.prompt}
                onChange={(event) => change({ prompt: event.target.value })}
                aria-describedby={errorsId("prompt")}
                className={`${fieldClass} py-2`}
              />
              <FieldErrors id={errorsId("prompt")} messages={byPath.prompt} />
            </div>
            {contentFields(form.type)}
            <FieldErrors id={errorsId("content")} messages={byPath.content} />
            <div className="flex flex-col gap-1">
              <label htmlFor={ids("code")} className="font-semibold">
                {text.code}
              </label>
              <select
                id={ids("code")}
                value={form.codeLanguage ?? ""}
                onChange={(event) =>
                  change({ codeLanguage: event.target.value === "" ? null : event.target.value })
                }
                aria-describedby={errorsId("code.language")}
                className={`${fieldClass} w-48`}
              >
                <option value="">{text.noCode}</option>
                {LANGUAGES.map((language) => (
                  <option key={language} value={language}>
                    {language}
                  </option>
                ))}
              </select>
              <FieldErrors id={errorsId("code.language")} messages={byPath["code.language"]} />
              {form.codeLanguage === null ? null : (
                <>
                  <label htmlFor={ids("codeText")} className="font-semibold">
                    {text.codeText}
                  </label>
                  <textarea
                    id={ids("codeText")}
                    rows={6}
                    value={form.codeText}
                    onChange={(event) => change({ codeText: event.target.value })}
                    aria-describedby={errorsId("code.text")}
                    className={`${fieldClass} py-2 font-mono`}
                  />
                  <FieldErrors id={errorsId("code.text")} messages={byPath["code.text"]} />
                </>
              )}
            </div>
            <div className="flex flex-col gap-1">
              <label htmlFor={ids("explanation")} className="font-semibold">
                {text.explanation}
              </label>
              <textarea
                id={ids("explanation")}
                rows={2}
                value={form.explanation}
                onChange={(event) => change({ explanation: event.target.value })}
                aria-describedby={errorsId("explanation")}
                className={`${fieldClass} py-2`}
              />
              <FieldErrors id={errorsId("explanation")} messages={byPath.explanation} />
            </div>
            <FieldErrors
              id={errorsId("general")}
              messages={general.map((issue) => issue.message)}
            />
            <div className="flex flex-wrap items-center gap-4">
              <ArcadeButton type="submit" disabled={busy}>
                {text.save}
              </ArcadeButton>
              {task === null ? null : (
                <ArcadeButton
                  variant="danger"
                  // Tasks in use can't be deleted (document 12, A-04; FR-071)
                  disabled={busy || usedBy.length > 0}
                  aria-describedby={ids("usedBy")}
                  onClick={() => void remove()}
                >
                  {text.delete}
                </ArcadeButton>
              )}
              <p id={ids("usedBy")} className="text-text-muted">
                {usedBy.length > 0 ? text.usedBy(usedBy.map((plan) => plan.name)) : null}
              </p>
            </div>
            <p role="status" className="font-semibold">
              {outcome.saved ? text.saved : outcome.message}
            </p>
          </form>
          <aside className="flex w-80 flex-col gap-2" aria-label={text.warnings}>
            {warnings.length === 0 ? (
              <p>{text.noWarnings}</p>
            ) : (
              <>
                <h2 className="font-semibold">{text.warnings}</h2>
                <ul className="list-disc pl-5 text-warning">
                  {warnings.map((warning) => (
                    <li key={`${warning.path}-${warning.code}`}>{warning.message}</li>
                  ))}
                </ul>
              </>
            )}
          </aside>
        </div>
      )}
    </AdminShell>
  );
}
