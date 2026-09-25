import Image from "next/image";
import type { ReactNode } from "react";

type SpeechBubbleProps = {
  name: string;
  characterRole: string;
  prompt: ReactNode;
  // A relative path to the self-hosted character image (NFR-24); without one, a text-only placeholder (R-11)
  imageSrc?: string;
  size?: "phone" | "projector";
};

// 96 px on phones and 160 px on the projector (document 12, section 5.5)
const artSizes = {
  phone: { px: 96, className: "size-24" },
  projector: { px: 160, className: "size-40" },
} as const;

// The character image, name and role, with the prompt in a bubble (document 12, section 5.7). The image is
// decorative, because the name is written beside it.
// TODO(US-22): the character art from the chosen pack (S0-03 T5)
export function SpeechBubble({
  name,
  characterRole,
  prompt,
  imageSrc,
  size = "phone",
}: SpeechBubbleProps) {
  const art = artSizes[size];
  return (
    <figure className="flex items-start gap-3">
      <div className="flex shrink-0 flex-col items-center gap-1">
        {imageSrc ? (
          <Image
            src={imageSrc}
            alt=""
            width={art.px}
            height={art.px}
            unoptimized
            className={`${art.className} [image-rendering:pixelated]`}
          />
        ) : (
          <div
            aria-hidden="true"
            data-art-placeholder
            className={`${art.className} flex items-center justify-center border-2 border-border bg-surface-2 font-display text-2xl text-accent`}
          >
            {name.charAt(0)}
          </div>
        )}
        <figcaption className="text-center">
          <span className="block font-semibold">{name}</span>
          <span className="block text-sm text-text-muted">{characterRole}</span>
        </figcaption>
      </div>
      <blockquote className="min-w-0 flex-1 border-2 border-border bg-surface p-3 text-lg">
        {prompt}
      </blockquote>
    </figure>
  );
}
