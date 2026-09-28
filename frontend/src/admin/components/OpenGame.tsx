import { copy } from "@/copy";
import { CopyButton } from "@/admin/components/CopyButton";
import { QrCode } from "@/ui/QrCode";
import type { GameView } from "@/types/dto";

const text = copy.admin.newGameScreen;

// The open game's code, join link and QR code, and the projector link, which opens in a new tab; each link can be
// copied (A-09). The heading is the live control screen's.
export function OpenGame({ game }: { game: GameView }) {
  return (
    <section className="flex flex-col gap-4 border-2 border-border bg-surface p-4">
      <div className="flex flex-wrap items-start gap-6">
        <QrCode value={game.joinUrl} label={text.qrLabel(game.code)} className="size-48" />
        <dl className="grid grid-cols-[auto_1fr] items-center gap-x-4 gap-y-2">
          <dt className="font-semibold">{text.code}</dt>
          <dd className="font-mono text-2xl">{game.code}</dd>
          <dt className="font-semibold">{text.joinLink}</dt>
          <dd className="flex flex-wrap items-center gap-2">
            <span className="font-mono break-all">{game.joinUrl}</span>
            <CopyButton value={game.joinUrl} label={copy.admin.liveControl.copyJoinLink} />
          </dd>
          <dt className="font-semibold">{text.projector}</dt>
          <dd className="flex flex-wrap items-center gap-2">
            <a
              href={game.projectorUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-primary underline"
            >
              {text.openProjector}
            </a>
            <CopyButton
              value={game.projectorUrl}
              label={copy.admin.liveControl.copyProjectorLink}
            />
          </dd>
        </dl>
      </div>
    </section>
  );
}
