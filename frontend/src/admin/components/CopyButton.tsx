"use client";

import { useEffect, useState } from "react";
import { copy } from "@/copy";
import { ArcadeButton } from "@/ui/ArcadeButton";

const text = copy.admin.liveControl;

/** How long "Copied!" shows, as for the phone's "Link copied!" (document 12, P-01). */
const COPIED_MS = 2_000;

// Copies a link to the clipboard and says so for 2 seconds, on screen and to screen readers; the link stays on screen
// for copying by hand (NFR-37, NFR-31)
export function CopyButton({ value, label }: { value: string; label: string }) {
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!copied) {
      return undefined;
    }
    const reset = setTimeout(() => setCopied(false), COPIED_MS);
    return () => clearTimeout(reset);
  }, [copied]);

  return (
    <>
      <ArcadeButton
        variant="secondary"
        aria-label={label}
        onClick={() => {
          navigator.clipboard?.writeText(value).then(
            () => setCopied(true),
            () => {
              // No clipboard access: the link is on screen to copy by hand
            },
          );
        }}
      >
        {copied ? text.copied : text.copy}
      </ArcadeButton>
      <span role="status" className="sr-only">
        {copied ? text.copied : ""}
      </span>
    </>
  );
}
