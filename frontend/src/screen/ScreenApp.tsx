"use client";

import { useSearchParams } from "next/navigation";
import { useEffect } from "react";
import { copy } from "@/copy";
import { useStomp } from "@/realtime/useStomp";
import { ProjectorShell } from "@/screen/ProjectorShell";
import { screenTopic, useScreenStore } from "@/screen/store";
import { EndedView } from "@/screen/views/EndedView";
import { GettingReadyView } from "@/screen/views/GettingReadyView";
import { assertNever } from "@/types/assertNever";
import { isScreenMessage } from "@/types/messages";

// The projector's entry point (LLD section 6.4). It connects with the key from ?key=, learns its game from the
// CONNECTED frame, and shows the view the server's SCREEN_STATE selects. It sends nothing but time-sync requests
// (LD-02), and the key never appears on the page.
export function ScreenApp() {
  // An empty ?key= is the same as none
  const key = useSearchParams().get("key") || null;
  const view = useScreenStore((state) => state.view);
  const endedMessage = useScreenStore((state) => state.endedMessage);
  const topic = useScreenStore(screenTopic);
  const connected = useScreenStore((state) => state.connected);
  const receive = useScreenStore((state) => state.receive);
  const refused = useScreenStore((state) => state.refused);

  // Without a key there is nothing to connect with: the same as a refused one (DI-76)
  useEffect(() => {
    if (key === null) {
      refused();
    }
  }, [key, refused]);

  useStomp({
    credentials: key === null ? null : { projectorKey: key },
    destinations: topic === null ? [] : [topic],
    onConnected: connected,
    onMessage: (_destination, message) => {
      if (isScreenMessage(message)) {
        receive(message);
      }
    },
    onRefused: refused,
  });

  switch (view) {
    case "connecting":
    case "gettingReady":
      return <GettingReadyView />;
    case "lobby":
      // TODO(S1-06 T7): LobbyView
      return <ProjectorShell title={copy.screen.scanToJoin} />;
    case "round":
      // TODO(US-39 and later): the practice, countdown, wall, reveal and winner views (S-03 to S-11)
      return <ProjectorShell title={copy.brand} />;
    case "ended":
      return <EndedView message={endedMessage ?? copy.screen.finished} />;
    default:
      return assertNever(view);
  }
}
