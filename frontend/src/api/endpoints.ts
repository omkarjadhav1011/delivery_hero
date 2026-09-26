import type {
  AdminSessionResponse,
  GameStatusResponse,
  JoinRequest,
  JoinResponse,
  TaskDetail,
  TaskFilter,
  TaskInput,
  TaskSummary,
} from "@/types/dto";
import { ADMIN_LOGIN_PATH, http } from "./http";

// Typed wrappers for each REST endpoint in document 11, section 7, built on http.ts

/** What the join screen shows before a name is typed (document 11, section 7.2). */
export function getGame(code: string): Promise<GameStatusResponse> {
  return http<GameStatusResponse>(`/api/games/${encodeURIComponent(code)}`);
}

/** Joins with the name as typed; the server tidies it and returns the final name and the token. */
export function joinGame(code: string, name: string): Promise<JoinResponse> {
  const body: JoinRequest = { name };
  return http<JoinResponse>(`/api/games/${encodeURIComponent(code)}/players`, {
    method: "POST",
    body,
  });
}

/** Whether an admin is logged in. It also sets the XSRF-TOKEN cookie that login needs (document 11, 5.2 and 7.3). */
export function getAdminSession(): Promise<AdminSessionResponse> {
  return http<AdminSessionResponse>("/api/admin/session");
}

/** Logs in the single admin user with the shared password; the server sets the DH_SESSION cookie. */
export function adminLogin(password: string): Promise<void> {
  return http<void>(ADMIN_LOGIN_PATH, {
    method: "POST",
    form: { username: "admin", password },
  });
}

/** Ends the admin session (document 11, section 7.3); like every state-changing admin request it sends CSRF. */
export function adminLogout(): Promise<void> {
  return http<void>("/api/admin/logout", { method: "POST" });
}

/** The task library, filtered on the server; blank filters and a blank search are left out (FR-070). */
export function listTasks(filter: TaskFilter): Promise<TaskSummary[]> {
  const query = new URLSearchParams();
  for (const [name, value] of Object.entries(filter)) {
    if (typeof value === "string" && value.trim() !== "") {
      query.set(name, value.trim());
    }
  }
  const search = query.toString();
  return http<TaskSummary[]>(search === "" ? "/api/admin/tasks" : `/api/admin/tasks?${search}`);
}

/** A task with its correct answers and the run plans that use it: admins only (document 11, section 7.4). */
export function getTask(id: string): Promise<TaskDetail> {
  return http<TaskDetail>(`/api/admin/tasks/${encodeURIComponent(id)}`);
}

export function createTask(input: TaskInput): Promise<TaskDetail> {
  return http<TaskDetail>("/api/admin/tasks", { method: "POST", body: input });
}

/** Saves a change; `input.version` is the version last read. */
export function updateTask(id: string, input: TaskInput): Promise<TaskDetail> {
  return http<TaskDetail>(`/api/admin/tasks/${encodeURIComponent(id)}`, {
    method: "PUT",
    body: input,
  });
}

/** Deletes a task no run plan uses; otherwise the server refuses with TASK_IN_USE. */
export function deleteTask(id: string, version: number): Promise<void> {
  return http<void>(`/api/admin/tasks/${encodeURIComponent(id)}?version=${version}`, {
    method: "DELETE",
  });
}
