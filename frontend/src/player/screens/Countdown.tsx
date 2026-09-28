import { copy } from "@/copy";
import { PhoneShell } from "@/player/PhoneShell";
import { countdownDigit, useCountdown } from "@/time/useCountdown";

type CountdownProps = {
  /** The round's start as server time, from GAME_STATE; null until it arrives. */
  startsAt: number | null;
};

// P-06 (document 12): the digit counts 5 to 1 from server time (FR-020), then the store switches to the task screen
// when the round goes live. It stays on 1 if the start passes before that message arrives.
export function Countdown({ startsAt }: CountdownProps) {
  const digit = countdownDigit(useCountdown(startsAt));
  return (
    <PhoneShell title={copy.countdown.title}>
      <p data-testid="countdown-digit" className="font-display text-6xl text-accent">
        {digit}
      </p>
      <p>{copy.countdown.tagline}</p>
    </PhoneShell>
  );
}
