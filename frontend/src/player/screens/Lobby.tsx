import { copy } from "@/copy";
import { PhoneShell } from "@/player/PhoneShell";

type LobbyProps = {
  name: string;
};

// P-04 (document 12): the player's final name while waiting; the store switches the screen when the host starts.
// TODO(EN-08): the four role characters once the art is chosen (S0-03 T5), and the slow pulse
export function Lobby({ name }: LobbyProps) {
  return (
    <PhoneShell title={copy.lobby.welcome(name)}>
      <p aria-live="polite">{copy.lobby.waiting}</p>
      <p className="text-sm text-text-muted">{copy.lobby.tip}</p>
    </PhoneShell>
  );
}
