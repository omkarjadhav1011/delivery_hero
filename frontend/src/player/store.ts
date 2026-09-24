// The player app's Zustand store (LLD section 6.3). Changes only through functions that apply a server message.
// TODO(US-04): create the store; the player token lives in localStorage as dh.token.<code>

export type PlayerScreen =
  | "checking"
  | "notice"
  | "joinForm"
  | "restoring"
  | "lobby"
  | "practice"
  | "countdown"
  | "task"
  | "lockout"
  | "incident"
  | "done"
  | "timesUp"
  | "results"
  | "finished"
  | "removed"
  | "ended";
