import { copy } from "@/copy";
import { ProjectorShell } from "@/screen/ProjectorShell";
import { Clock } from "@/screen/views/Clock";
import { PhaseBar } from "@/screen/views/PhaseBar";
import type { ScreenRound } from "@/types/messages";

type LiveViewProps = {
  round: ScreenRound;
};

// S-05's header: the phase bar and the clock beside the brand (FR-023, FR-054).
// TODO(US-39, US-40, US-41): the wall, the top 10 and the live feed
export function LiveView({ round }: LiveViewProps) {
  return (
    <ProjectorShell
      header={
        <>
          <PhaseBar phases={round.phases} />
          <Clock endsAt={round.endsAt} />
        </>
      }
    >
      <h1 className="sr-only">{copy.brand}</h1>
    </ProjectorShell>
  );
}
