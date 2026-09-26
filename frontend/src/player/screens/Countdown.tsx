import { copy } from "@/copy";
import { PhoneShell } from "@/player/PhoneShell";

// P-06 (document 12), a shell until the round can start. TODO(US-13): the digit counting 5 to 1 from server time
export function Countdown() {
  return (
    <PhoneShell title={copy.countdown.title}>
      <p>{copy.countdown.tagline}</p>
    </PhoneShell>
  );
}
