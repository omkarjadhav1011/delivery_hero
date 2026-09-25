"use client";

import { useSearchParams } from "next/navigation";
import { copy } from "@/copy";
import { PlaceholderPage } from "@/ui/PlaceholderPage";

// The phone app's entry point (LLD section 6.3).
// TODO(US-01): check the browser and the game code, then show the join form (P-01 to P-04)
export function PlayerApp() {
  const code = useSearchParams().get("code");
  return (
    <div data-game-code={code ?? undefined}>
      <PlaceholderPage title={copy.join.title} />
    </div>
  );
}
