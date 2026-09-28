import { copy } from "@/copy";
import { ProjectorShell } from "@/screen/ProjectorShell";
import { countdownDigit, useCountdown } from "@/time/useCountdown";

type CountdownViewProps = {
  /** The round's start as server time, from SCREEN_STATE. */
  startsAt: number;
};

// S-04: the digit counting 5 to 1 from server time, above "The sprint starts now!" (FR-019, FR-054)
export function CountdownView({ startsAt }: CountdownViewProps) {
  const digit = countdownDigit(useCountdown(startsAt));
  return (
    <ProjectorShell>
      <div className="flex flex-1 flex-col items-center justify-center gap-8 text-center">
        <p className="font-display text-9xl text-accent">{digit}</p>
        <h1 className="text-5xl font-semibold">{copy.screen.sprintStarts}</h1>
      </div>
    </ProjectorShell>
  );
}
