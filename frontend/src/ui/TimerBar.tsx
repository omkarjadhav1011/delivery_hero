import { PixelIcon } from "./PixelIcon";

type TimerBarProps = { secondsLeft: number; totalSeconds: number };

// The bar's color: --warning at 5 s and --danger at 3 s (UX-04)
export function timerTone(secondsLeft: number): "primary" | "warning" | "danger" {
  if (secondsLeft <= 3) return "danger";
  if (secondsLeft <= 5) return "warning";
  return "primary";
}

const toneClasses = {
  primary: "text-primary [&::-webkit-progress-value]:bg-primary",
  warning: "text-warning [&::-webkit-progress-value]:bg-warning",
  danger: "text-danger [&::-webkit-progress-value]:bg-danger",
} as const;

// A draining bar plus the seconds as a number (document 12, section 5.7). Visuals only: the countdown and its
// live-region announcements arrive with US-16.
// TODO(US-16): drive it from useCountdown and announce every 5 seconds
export function TimerBar({ secondsLeft, totalSeconds }: TimerBarProps) {
  const tone = timerTone(secondsLeft);
  const seconds = Math.max(0, Math.ceil(secondsLeft));
  return (
    <div data-tone={tone} className={`flex items-center gap-2 ${toneClasses[tone]}`}>
      <PixelIcon name="clock" decorative />
      <progress
        aria-hidden="true"
        value={Math.max(0, secondsLeft)}
        max={totalSeconds}
        className="h-3 min-w-0 flex-1 appearance-none border-2 border-border bg-surface [&::-webkit-progress-bar]:bg-surface"
      />
      <span className="font-display text-base tabular-nums">{seconds}</span>
    </div>
  );
}
