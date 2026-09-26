"use client";

import { useEffect, useId, useState, type FormEvent } from "react";
import { getGame, joinGame } from "@/api/endpoints";
import { ApiError } from "@/api/http";
import { copy } from "@/copy";
import { PhoneShell } from "@/player/PhoneShell";
import type { JoinRefusal, JoinResponse } from "@/types/dto";
import { ArcadeButton } from "@/ui/ArcadeButton";

type JoinScreenProps = {
  /** The `?code=` of the join link, or null when the link has none. */
  code: string | null;
  onJoined: (joined: JoinResponse) => void;
};

type View =
  | { kind: "checking" }
  | { kind: "form"; submitting: boolean; invalidName: boolean }
  | { kind: "message"; reason: JoinRefusal };

const REFUSALS: ReadonlySet<string> = new Set<JoinRefusal>([
  "GAME_NOT_ACTIVE",
  "LOBBY_NOT_OPEN",
  "JOINING_CLOSED",
  "GAME_FULL",
  "RATE_LIMITED",
]);

function refusalOf(error: unknown): JoinRefusal | null {
  return error instanceof ApiError && error.code !== null && REFUSALS.has(error.code)
    ? (error.code as JoinRefusal)
    : null;
}

// P-02 and P-03 (document 12): checks the game, asks for a name, and shows why joining isn't possible.
// TODO(US-05): the icons and the "Try again" button of P-03
export function JoinScreen({ code, onJoined }: JoinScreenProps) {
  const [view, setView] = useState<View>(
    code === null ? { kind: "message", reason: "GAME_NOT_ACTIVE" } : { kind: "checking" },
  );
  const [name, setName] = useState("");
  const hintId = useId();
  const errorId = useId();

  useEffect(() => {
    if (code === null) {
      return undefined;
    }
    let cancelled = false;
    getGame(code).then(
      (game) => {
        if (!cancelled) {
          setView(
            game.reason === null
              ? { kind: "form", submitting: false, invalidName: false }
              : { kind: "message", reason: game.reason },
          );
        }
      },
      (error: unknown) => {
        if (!cancelled) {
          // Without an answer, the form lets the player try; joining gives the server's reason
          const reason = refusalOf(error);
          setView(
            reason === null
              ? { kind: "form", submitting: false, invalidName: false }
              : { kind: "message", reason },
          );
        }
      },
    );
    return () => {
      cancelled = true;
    };
  }, [code]);

  if (view.kind === "checking") {
    return <PhoneShell title={copy.join.title} />;
  }
  if (view.kind === "message") {
    return (
      <PhoneShell title={copy.brand}>
        <p role="status">{copy.joinMessages[view.reason]}</p>
      </PhoneShell>
    );
  }

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (code === null || name.trim() === "" || view.submitting) {
      return;
    }
    setView({ kind: "form", submitting: true, invalidName: false });
    joinGame(code, name).then(onJoined, (error: unknown) => {
      const reason = refusalOf(error);
      if (reason !== null) {
        setView({ kind: "message", reason });
      } else {
        const invalidName = error instanceof ApiError && error.code === "INVALID_NAME";
        setView({ kind: "form", submitting: false, invalidName });
      }
    });
  };

  const describedBy = view.invalidName ? `${hintId} ${errorId}` : hintId;
  return (
    <PhoneShell title={copy.join.title}>
      <form className="flex w-full flex-col gap-3 text-left" onSubmit={submit} noValidate>
        <input
          type="text"
          name="name"
          aria-label={copy.join.title}
          aria-describedby={describedBy}
          aria-invalid={view.invalidName}
          autoComplete="nickname"
          className="min-h-12 w-full border-2 border-border bg-surface px-3 text-base text-text"
          value={name}
          onChange={(event) => setName(event.target.value)}
        />
        <p id={hintId} className="text-sm text-text-muted">
          {copy.join.hint}
        </p>
        {view.invalidName && (
          <p id={errorId} className="text-sm text-danger">
            {copy.join.invalidName}
          </p>
        )}
        <ArcadeButton type="submit" disabled={name.trim() === "" || view.submitting}>
          {copy.join.submit}
        </ArcadeButton>
      </form>
      <p className="text-sm text-text-muted">{copy.join.privacy}</p>
    </PhoneShell>
  );
}
