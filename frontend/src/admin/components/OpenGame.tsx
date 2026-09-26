import { copy } from "@/copy";
import { QrCode } from "@/ui/QrCode";
import type { GameView } from "@/types/dto";

const text = copy.admin.newGameScreen;

// The open game on A-08: its code, join link and QR code, and the projector link, which opens in a new tab
export function OpenGame({ game }: { game: GameView }) {
  return (
    <section className="flex flex-col gap-4 border-2 border-border bg-surface p-4">
      <h2 className="text-lg font-semibold">{text.game(game.code, game.runPlanName)}</h2>
      <div className="flex flex-wrap items-start gap-6">
        <QrCode value={game.joinUrl} label={text.qrLabel(game.code)} className="size-48" />
        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2">
          <dt className="font-semibold">{text.code}</dt>
          <dd className="font-mono text-2xl">{game.code}</dd>
          <dt className="font-semibold">{text.joinLink}</dt>
          <dd className="font-mono break-all">{game.joinUrl}</dd>
          <dt className="font-semibold">{text.projector}</dt>
          <dd>
            <a
              href={game.projectorUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-primary underline"
            >
              {text.openProjector}
            </a>
          </dd>
        </dl>
      </div>
    </section>
  );
}
