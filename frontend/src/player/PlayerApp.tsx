"use client";

import { useSearchParams } from "next/navigation";
import { useState } from "react";
import { copy } from "@/copy";
import { PhoneShell } from "@/player/PhoneShell";
import { JoinScreen } from "@/player/screens/JoinScreen";
import { saveToken } from "@/player/session";
import type { JoinResponse } from "@/types/dto";

// The phone app's entry point (LLD section 6.3).
// TODO(US-38): the Chrome check (P-01); TODO(US-05): restoring a saved token instead of asking for a name again
export function PlayerApp() {
  const code = useSearchParams().get("code");
  const [joined, setJoined] = useState<JoinResponse | null>(null);

  if (joined === null || code === null) {
    return (
      <JoinScreen
        code={code}
        onJoined={(result) => {
          if (code !== null) {
            saveToken(code, result.token);
          }
          setJoined(result);
        }}
      />
    );
  }
  // TODO(US-04): the player store, the connection and the lobby
  return <PhoneShell title={copy.lobby.welcome(joined.name)} />;
}
