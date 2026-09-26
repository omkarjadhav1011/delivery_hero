import type { GameStatusResponse, JoinRequest, JoinResponse } from "@/types/dto";
import { http } from "./http";

// Typed wrappers for each REST endpoint in document 11, section 7, built on http.ts
// TODO(US-49): the admin session endpoints

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
