import { copy } from "@/copy";
import { ProjectorShell } from "@/screen/ProjectorShell";
import type { ScreenPlayer } from "@/types/messages";
import { QrCode } from "@/ui/QrCode";

type LobbyViewProps = {
  joinUrl: string;
  /** Newest first (FR-053). */
  players: readonly ScreenPlayer[];
  playerCount: number;
};

// S-02: the join QR code at 400 × 400 px or more, the join URL as text, and the joined names, newest first, popping
// in as they arrive (FR-053, document 12 S-02)
export function LobbyView({ joinUrl, players, playerCount }: LobbyViewProps) {
  return (
    <ProjectorShell
      title={copy.screen.scanToJoin}
      sidebar={
        <div className="flex flex-col gap-4">
          <p className="font-display text-3xl">{copy.screen.joined(playerCount)}</p>
          <ul className="flex flex-wrap gap-x-6 gap-y-2 text-3xl">
            {players.map((player) => (
              <li key={player.playerId} className="animate-fade-in">
                {player.firstName}
              </li>
            ))}
          </ul>
        </div>
      }
    >
      <QrCode value={joinUrl} label={copy.screen.qrLabel} className="size-100" />
      <p className="text-3xl break-all">{joinUrl}</p>
      <p className="text-3xl">{copy.screen.openInChrome}</p>
    </ProjectorShell>
  );
}
