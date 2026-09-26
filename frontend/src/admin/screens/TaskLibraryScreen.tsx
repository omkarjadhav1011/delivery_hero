"use client";

import Link from "next/link";
import { useEffect, useId, useState } from "react";
import { listTasks } from "@/api/endpoints";
import { copy } from "@/copy";
import { AdminShell } from "@/admin/components/AdminShell";
import { fieldClass } from "@/admin/components/FieldErrors";
import { KINDS, PHASES, ROLES, TYPES } from "@/admin/taskForm";
import type { TaskFilter, TaskSummary } from "@/types/dto";

const text = copy.admin.taskLibrary;
const editor = copy.admin.taskEditor;

// What the controls hold: "" is All, or no search
type Filters = { [K in keyof TaskFilter]-?: NonNullable<TaskFilter[K]> | "" };
const NO_FILTERS: Filters = { role: "", phase: "", kind: "", type: "", q: "" };

function asFilter({ role, phase, kind, type, q }: Filters): TaskFilter {
  return {
    ...(role === "" ? {} : { role }),
    ...(phase === "" ? {} : { phase }),
    ...(kind === "" ? {} : { kind }),
    ...(type === "" ? {} : { type }),
    ...(q === "" ? {} : { q }),
  };
}

type Listing = { tasks: readonly TaskSummary[]; failed: boolean; loading: boolean };

const cell = "border-b-2 border-border px-3 py-2 text-left align-top";

// A-03 Task library (document 12, section 9; FR-070): the server filters and searches, and selecting a task's key
// opens it in A-04.
export function TaskLibraryScreen() {
  const [filters, setFilters] = useState<Filters>(NO_FILTERS);
  const [listing, setListing] = useState<Listing>({ tasks: [], failed: false, loading: true });
  const baseId = useId();

  useEffect(() => {
    // Only the answer to the latest filters is shown, whatever order the answers arrive in
    let cancelled = false;
    listTasks(asFilter(filters)).then(
      (tasks) => {
        if (!cancelled) {
          setListing({ tasks, failed: false, loading: false });
        }
      },
      () => {
        if (!cancelled) {
          setListing({ tasks: [], failed: true, loading: false });
        }
      },
    );
    return () => {
      cancelled = true;
    };
  }, [filters]);

  function select<F extends "role" | "phase" | "kind" | "type", T extends Filters[F] & string>(
    field: F,
    label: string,
    values: readonly T[],
    names: Record<T, string>,
  ) {
    const id = `${baseId}-${field}`;
    return (
      <div className="flex flex-col gap-1">
        <label htmlFor={id} className="font-semibold">
          {label}
        </label>
        <select
          id={id}
          value={filters[field]}
          onChange={(event) =>
            // The options are "" and the values given, so the choice is one of them
            setFilters((current) => ({ ...current, [field]: event.target.value as T | "" }))
          }
          className={fieldClass}
        >
          <option value="">{text.all}</option>
          {values.map((value) => (
            <option key={value} value={value}>
              {names[value]}
            </option>
          ))}
        </select>
      </div>
    );
  }

  const { tasks, failed, loading } = listing;
  let status: string | null = null;
  if (failed) {
    status = text.failed;
  } else if (!loading && tasks.length === 0) {
    status = text.none;
  }

  return (
    <AdminShell title={copy.admin.nav.tasks}>
      <div className="flex flex-col gap-4">
        <div className="flex flex-wrap items-end gap-4">
          {select("role", editor.role, ROLES, editor.roles)}
          {select("phase", editor.phase, PHASES, editor.phases)}
          {select("kind", editor.kind, KINDS, editor.kinds)}
          {select("type", editor.type, TYPES, editor.types)}
          <div className="flex flex-col gap-1">
            <label htmlFor={`${baseId}-q`} className="font-semibold">
              {text.search}
            </label>
            <input
              id={`${baseId}-q`}
              type="search"
              value={filters.q}
              onChange={(event) => setFilters((current) => ({ ...current, q: event.target.value }))}
              className={fieldClass}
            />
          </div>
          <Link
            href="/admin/tasks/edit/"
            className="ml-auto inline-flex min-h-12 items-center border-2 border-primary bg-primary px-4 py-2 font-semibold text-ink"
          >
            {editor.newTask}
          </Link>
        </div>
        <p role="status" className="font-semibold">
          {status}
        </p>
        {tasks.length === 0 ? null : (
          <table className="w-full border-collapse">
            <thead>
              <tr>
                <th scope="col" className={cell}>
                  {text.key}
                </th>
                <th scope="col" className={cell}>
                  {editor.role}
                </th>
                <th scope="col" className={cell}>
                  {editor.phase}
                </th>
                <th scope="col" className={cell}>
                  {editor.type}
                </th>
                <th scope="col" className={cell}>
                  {text.usedIn}
                </th>
                <th scope="col" className={cell}>
                  {text.time}
                </th>
              </tr>
            </thead>
            {tasks.map((task) => (
              // Each task is its own body: the row of fields, then the prompt under it (document 12, A-03)
              <tbody key={task.id}>
                <tr>
                  <th
                    scope="row"
                    id={`${baseId}-${task.id}`}
                    className={`${cell} border-b-0 font-mono`}
                  >
                    <Link
                      href={`/admin/tasks/edit/?id=${encodeURIComponent(task.id)}`}
                      className="text-primary underline"
                    >
                      {task.key}
                    </Link>
                  </th>
                  <td className={`${cell} border-b-0`}>{editor.roles[task.role]}</td>
                  <td className={`${cell} border-b-0`}>
                    {task.phase === null ? text.noPhase : editor.phases[task.phase]}
                  </td>
                  <td className={`${cell} border-b-0`}>{editor.types[task.type]}</td>
                  <td className={`${cell} border-b-0`}>{text.plans(task.usedByCount)}</td>
                  <td className={`${cell} border-b-0`}>
                    {text.seconds(task.effectiveTimeLimitSeconds)}
                  </td>
                </tr>
                <tr>
                  <td
                    colSpan={6}
                    headers={`${baseId}-${task.id}`}
                    className={`${cell} text-text-muted`}
                  >
                    {task.prompt}
                  </td>
                </tr>
              </tbody>
            ))}
          </table>
        )}
      </div>
    </AdminShell>
  );
}
