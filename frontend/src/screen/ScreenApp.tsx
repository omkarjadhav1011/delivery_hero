"use client";

import { useSearchParams } from "next/navigation";
import { copy } from "@/copy";
import { PlaceholderPage } from "@/ui/PlaceholderPage";

// The projector's entry point (LLD section 6.4). The projector key is a secret, so it never reaches the page.
// TODO(US-37): connect with the key in the STOMP CONNECT headers and show S-01 to S-11
export function ScreenApp() {
  const hasKey = useSearchParams().has("key");
  return (
    <div data-projector-key-present={hasKey}>
      <PlaceholderPage title={copy.screen.gettingReady} />
    </div>
  );
}
