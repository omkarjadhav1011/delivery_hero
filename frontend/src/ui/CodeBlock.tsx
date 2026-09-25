"use client";

import { useLayoutEffect, useRef, useState } from "react";

type CodeBlockProps = { code: string; label: string };

// Monospace code with its own horizontal scroll (FR-033) and a fade at the edge while there's more to see
// (document 12, section 5.7). The page itself never scrolls sideways (NFR-30). The region takes focus so a
// keyboard can scroll it.
export function CodeBlock({ code, label }: CodeBlockProps) {
  const regionRef = useRef<HTMLDivElement>(null);
  const [hasMore, setHasMore] = useState(false);

  useLayoutEffect(() => {
    const region = regionRef.current;
    if (!region) return;
    const update = () =>
      setHasMore(region.scrollLeft + region.clientWidth < region.scrollWidth - 1);
    update();
    region.addEventListener("scroll", update, { passive: true });
    const observer = typeof ResizeObserver === "undefined" ? undefined : new ResizeObserver(update);
    observer?.observe(region);
    return () => {
      region.removeEventListener("scroll", update);
      observer?.disconnect();
    };
  }, [code]);

  return (
    <div className="relative max-w-full border-2 border-border bg-surface">
      <div
        ref={regionRef}
        role="region"
        aria-label={label}
        // A scrollable region must take keyboard focus (axe scrollable-region-focusable; NFR-32)
        // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
        tabIndex={0}
        className="overflow-x-auto"
      >
        <pre className="p-3 font-mono text-sm leading-relaxed text-text">
          <code>{code}</code>
        </pre>
      </div>
      {hasMore && (
        <div
          data-edge-fade
          aria-hidden="true"
          className="pointer-events-none absolute inset-y-0 right-0 w-8 bg-linear-to-l from-surface"
        />
      )}
    </div>
  );
}
