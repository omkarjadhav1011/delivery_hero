import { copy } from "@/copy";
import { formatRemaining, useCountdown } from "@/time/useCountdown";
import { PixelIcon } from "@/ui/PixelIcon";

type ClockProps = {
  /** The round's end as server time. */
  endsAt: number;
};

// S-05's clock: the time left in the round as m:ss, on the server's clock (FR-020, FR-054)
export function Clock({ endsAt }: ClockProps) {
  const remaining = useCountdown(endsAt) ?? 0;
  return (
    <p className="flex items-center gap-3 font-display text-4xl">
      <PixelIcon name="clock" label={copy.screen.timeLeftLabel} className="size-10" />
      <span>{formatRemaining(remaining)}</span>
    </p>
  );
}
