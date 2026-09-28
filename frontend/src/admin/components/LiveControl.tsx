"use client";

import { useState } from "react";
import { performHostAction } from "@/api/endpoints";
import { copy } from "@/copy";
import { adminTopic, useAdminStore } from "@/admin/store";
import { LiveStats } from "@/admin/components/LiveStats";
import { OpenGame } from "@/admin/components/OpenGame";
import { type UseStompOptions, useStomp } from "@/realtime/useStomp";
import { formatRemaining, useCountdown } from "@/time/useCountdown";
import type { GameState, GameView, HostAction } from "@/types/dto";
import { isAdminMessage } from "@/types/messages";
import { ArcadeButton } from "@/ui/ArcadeButton";

const text = copy.admin.liveControl;

/** The states from which the reveal buttons are shown (document 12, A-09). */
const REVEAL_STATES: ReadonlySet<GameState> = new Set(["ENDED", "REVEAL", "RESULTS"]);

/** The main buttons, always shown and enabled only when the action is valid now (FR-080). */
const MAIN_BUTTONS: readonly { action: HostAction; label: string }[] = [
  { action: "OPEN_LOBBY", label: text.openLobby },
  { action: "START_PRACTICE", label: text.startPractice },
  { action: "END_PRACTICE", label: text.endPractice },
  { action: "START_ROUND", label: text.startRound },
];

const REVEAL_BUTTONS: readonly { action: HostAction; label: string }[] = [
  { action: "START_REVEAL", label: text.startReveal },
  { action: "PREVIOUS_STEP", label: text.back },
  { action: "NEXT_STEP", label: text.next },
];

function header(game: GameView): string {
  const state = game.liveDetailsAvailable ? game.state : text.resultsLost;
  return text.header(game.code, game.runPlanName, state);
}

// A-09 Live control (document 12, section 9; FR-080 to FR-082): the open game's links, the host actions valid in its
// state and the live statistics from LIVE_STATS on the admin topic. TODO(US-61, US-09, DEC-112): the Void buttons,
// the lobby's player list and the reveal's keyboard shortcuts.
export function LiveControl({ createClient }: { createClient?: UseStompOptions["createClient"] }) {
  const game = useAdminStore((state) => state.game);
  const stats = useAdminStore((state) => state.stats);
  const topic = useAdminStore(adminTopic);
  const receive = useAdminStore((state) => state.receive);
  const actionApplied = useAdminStore((state) => state.actionApplied);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const remaining = useCountdown(stats?.round?.endsAt ?? null);

  useStomp({
    credentials: { admin: true },
    destinations: topic === null ? [] : [topic],
    onMessage: (_destination, message) => {
      if (isAdminMessage(message)) {
        receive(message);
      }
    },
    createClient,
  });

  if (game === null) {
    return null;
  }
  const gameId = game.id;
  const allowed = new Set(game.allowedActions);

  async function perform(action: HostAction) {
    setBusy(true);
    setNotice(null);
    try {
      actionApplied(await performHostAction(gameId, { action }));
    } catch {
      setNotice(text.failed);
    } finally {
      setBusy(false);
    }
  }

  function button(action: HostAction, label: string, variant: "primary" | "danger" = "primary") {
    return (
      <ArcadeButton
        key={action}
        variant={variant}
        disabled={busy || !allowed.has(action)}
        onClick={() => void perform(action)}
      >
        {label}
      </ArcadeButton>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-baseline justify-between gap-4">
        <h2 className="text-lg font-semibold">{header(game)}</h2>
        {remaining === null ? null : (
          <p className="font-mono text-lg">{text.timeLeft(formatRemaining(remaining))}</p>
        )}
      </div>
      <OpenGame game={game} />
      <section aria-label={text.actions} className="flex flex-col gap-3">
        <div className="flex flex-wrap gap-3">
          {MAIN_BUTTONS.map(({ action, label }) => button(action, label))}
        </div>
        {REVEAL_STATES.has(game.state) ? (
          <div className="flex flex-wrap gap-3">
            {REVEAL_BUTTONS.map(({ action, label }) => button(action, label))}
          </div>
        ) : null}
        <div>
          {game.state === "RESULTS"
            ? button("CLOSE", text.closeEvent, "danger")
            : button("CANCEL", text.cancelGame, "danger")}
        </div>
      </section>
      <p role="status" className="font-semibold">
        {notice}
      </p>
      {stats === null ? null : <LiveStats stats={stats} />}
    </div>
  );
}
