import { copy } from "@/copy";
import { ProjectorShell } from "@/screen/ProjectorShell";

// S-01, while the game is in Created: no QR code yet, so nobody tries to join too early (DEC-170, UX-05).
// TODO(EN-08): the four role characters once the art is chosen (S0-03 T5)
export function GettingReadyView() {
  return <ProjectorShell title={copy.screen.gettingReady} />;
}
