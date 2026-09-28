import { copy } from "@/copy";
import { useServerNow } from "@/time/useCountdown";
import type { PhaseStart } from "@/types/messages";

type PhaseBarProps = {
  /** The round's phase starts, in order, from SCREEN_STATE. */
  phases: readonly PhaseStart[];
};

// S-05's phase bar: the clock's phase has a filled marker and a bold label (FR-023). It follows server time and the
// round's phase starts only, never what phase players are on.
export function PhaseBar({ phases }: PhaseBarProps) {
  const now = useServerNow();
  const current = phases.findLast((phase) => phase.startsAt <= now)?.phase ?? null;
  return (
    <ol aria-label={copy.screen.phaseBarLabel} className="flex items-center gap-6 text-2xl">
      {phases.map(({ phase }) => {
        const active = phase === current;
        return (
          <li
            key={phase}
            aria-current={active ? "step" : undefined}
            className="flex items-center gap-2"
          >
            <span
              aria-hidden="true"
              data-phase-marker={active ? "" : undefined}
              className={`size-4 border-2 border-current ${active ? "bg-accent" : ""}`}
            />
            <span className={active ? "font-bold text-accent" : "text-text-muted"}>
              {copy.screen.phases[phase]}
            </span>
          </li>
        );
      })}
    </ol>
  );
}
