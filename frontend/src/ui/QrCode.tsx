import QRCode from "qrcode";
import { useMemo } from "react";

/** The quiet zone around the code, in modules, as the QR standard asks. */
const MARGIN = 4;

/** A game's join URL: `https://<host>/join?code=<CODE>` (FR-001, DEC-99). */
export function joinUrl(origin: string, code: string): string {
  return `${origin}/join?code=${encodeURIComponent(code)}`;
}

type QrCodeProps = {
  /** The exact text to encode: the game's join URL. */
  value: string;
  /** The accessible name of the image. */
  label: string;
  className?: string;
};

// The join URL's QR code, generated in the browser so nothing leaves the site (NFR-24, DEC-134). Drawn as one SVG
// path of dark modules on a light square, so it scales to any size without blurring. The projector lobby (S1-06)
// shows it at 400 px or more.
export function QrCode({ value, label, className = "" }: QrCodeProps) {
  const { size, d } = useMemo(() => {
    const modules = QRCode.create(value, { errorCorrectionLevel: "M" }).modules;
    let path = "";
    for (let y = 0; y < modules.size; y++) {
      for (let x = 0; x < modules.size; x++) {
        if (modules.get(y, x)) {
          path += `M${x + MARGIN} ${y + MARGIN}h1v1h-1z`;
        }
      }
    }
    return { size: modules.size + 2 * MARGIN, d: path };
  }, [value]);

  return (
    <svg
      role="img"
      aria-label={label}
      viewBox={`0 0 ${size} ${size}`}
      shapeRendering="crispEdges"
      className={className}
    >
      <rect width={size} height={size} className="fill-text" />
      <path d={d} className="fill-ink" />
    </svg>
  );
}
