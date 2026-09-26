"use client";

import { useEffect, useId, useState } from "react";
import { createGame, getCurrentGame, listRunPlans } from "@/api/endpoints";
import { ApiError } from "@/api/http";
import { copy } from "@/copy";
import { AdminShell } from "@/admin/components/AdminShell";
import { fieldClass } from "@/admin/components/FieldErrors";
import { OpenGame } from "@/admin/components/OpenGame";
import { PlanStatus } from "@/admin/components/PlanStatus";
import { ArcadeButton } from "@/ui/ArcadeButton";
import { PixelIcon } from "@/ui/PixelIcon";
import type { GameView, RunPlanSummary, ValidationIssue } from "@/types/dto";

const text = copy.admin.newGameScreen;

type Loaded =
  | { status: "loading" }
  | { status: "failed" }
  | { status: "ready"; plans: readonly RunPlanSummary[]; game: GameView | null };

type Refusal = { message: string; reasons: readonly ValidationIssue[] } | null;

// A-08 New game (document 12, section 9; FR-079): pick a run plan and create the game, then show its code, join
// link, QR code and projector link. While a game is open it's shown instead of the form (DEC-101); the live
// controls around it are A-09 (US-60). Test games are US-63.
export function NewGameScreen() {
  const [loaded, setLoaded] = useState<Loaded>({ status: "loading" });
  const [chosen, setChosen] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [refusal, setRefusal] = useState<Refusal>(null);
  const planId = useId();

  useEffect(() => {
    let cancelled = false;
    Promise.all([listRunPlans(), getCurrentGame()]).then(
      ([plans, game]) => {
        if (!cancelled) {
          setLoaded({ status: "ready", plans, game });
        }
      },
      () => {
        if (!cancelled) {
          setLoaded({ status: "failed" });
        }
      },
    );
    return () => {
      cancelled = true;
    };
  }, []);

  if (loaded.status !== "ready") {
    return (
      <AdminShell title={copy.admin.newGame}>
        <p role="status" className="font-semibold">
          {loaded.status === "failed" ? text.failed : null}
        </p>
      </AdminShell>
    );
  }

  const { plans, game } = loaded;
  const plan = plans.find((candidate) => candidate.id === chosen) ?? plans[0];

  async function create(runPlanId: string) {
    setCreating(true);
    setRefusal(null);
    try {
      const created = await createGame(runPlanId);
      setLoaded({ status: "ready", plans, game: created });
    } catch (error) {
      await refused(error);
    } finally {
      setCreating(false);
    }
  }

  async function refused(error: unknown) {
    if (error instanceof ApiError && error.code === "ANOTHER_GAME_OPEN") {
      // Someone else created a game first: show theirs, as the link to it (AC-US59-03)
      setRefusal({ message: text.anotherGameOpen, reasons: [] });
      try {
        setLoaded({ status: "ready", plans, game: await getCurrentGame() });
      } catch {
        setRefusal({ message: text.failed, reasons: [] });
      }
    } else if (error instanceof ApiError && error.code === "VALIDATION_FAILED") {
      setRefusal({ message: text.planErrors, reasons: error.errors });
      await reloadPlans();
    } else if (error instanceof ApiError && error.code === "NOT_FOUND") {
      setRefusal({ message: text.planGone, reasons: [] });
      await reloadPlans();
    } else {
      setRefusal({ message: text.failed, reasons: [] });
    }
  }

  // The plan changed or went since the list loaded, so its counts are stale
  async function reloadPlans() {
    try {
      const fresh = await listRunPlans();
      setLoaded((current) => (current.status === "ready" ? { ...current, plans: fresh } : current));
    } catch {
      // The refusal already on screen says enough; the old list stays
    }
  }

  return (
    <AdminShell title={copy.admin.newGame}>
      <div className="flex flex-col gap-4">
        {/* Always on the page, so screen readers announce a refusal when it appears (NFR-31) */}
        <div role="status" className="font-semibold">
          {refusal === null ? null : (
            <>
              {/* Colour marks only the icon, as in PlanStatus (document 12, section 5.2) */}
              <p className="flex items-center gap-2">
                <PixelIcon name="cross" decorative className="size-6 text-danger" />
                {refusal.message}
              </p>
              {refusal.reasons.length > 0 ? (
                <ul className="list-disc pl-6">
                  {refusal.reasons.map((issue, index) => (
                    <li key={`${issue.path}-${issue.code}-${index}`}>{issue.message}</li>
                  ))}
                </ul>
              ) : null}
            </>
          )}
        </div>
        {game === null ? (
          <div className="flex flex-col gap-4">
            <div className="flex flex-wrap items-end gap-4">
              <div className="flex flex-col gap-1">
                <label htmlFor={planId} className="font-semibold">
                  {text.runPlan}
                </label>
                <select
                  id={planId}
                  value={plan?.id ?? ""}
                  onChange={(event) => {
                    // A refusal belongs to the plan it was for
                    setChosen(event.target.value);
                    setRefusal(null);
                  }}
                  className={fieldClass}
                  disabled={plans.length === 0}
                >
                  {plans.map((candidate) => (
                    <option key={candidate.id} value={candidate.id}>
                      {candidate.name}
                    </option>
                  ))}
                </select>
              </div>
              <PlanStatus plan={plan} />
            </div>
            <div>
              <ArcadeButton
                disabled={plan === undefined || plan.errorCount > 0 || creating}
                onClick={() => {
                  if (plan !== undefined) {
                    void create(plan.id);
                  }
                }}
              >
                {creating ? text.creating : text.create}
              </ArcadeButton>
            </div>
          </div>
        ) : (
          <OpenGame game={game} />
        )}
      </div>
    </AdminShell>
  );
}
