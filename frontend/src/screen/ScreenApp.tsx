"use client";

import { useSearchParams } from "next/navigation";
import { copy } from "@/copy";
import { type UseStompOptions, useStomp } from "@/realtime/useStomp";
import { ProjectorShell } from "@/screen/ProjectorShell";
import { screenTopic, useScreenStore } from "@/screen/store";
import { EndedView } from "@/screen/views/EndedView";
import { GettingReadyView } from "@/screen/views/GettingReadyView";
import { LobbyView } from "@/screen/views/LobbyView";
import { assertNever } from "@/types/assertNever";
import { isScreenMessage } from "@/types/messages";

// The projector's entry point (LLD section 6.4). It connects with the key from ?key=, learns its game from the
// CONNECTED frame, and shows the view the server's SCREEN_STATE selects. It sends nothing but time-sync requests
// (LD-02), and the key never appears on the page.
export function ScreenApp({ createClient }: { createClient?: UseStompOptions["createClient"] }) {
  // An empty ?key= is the same as none
  const key = useSearchParams().get("key") || null;
  const storedView = useScreenStore((state) => state.view);
  // Without a key there is nothing to connect with: the same as a refused one (DI-79)
  const view = key === null ? "ended" : storedView;
  const endedMessage = useScreenStore((state) => state.endedMessage);
  const joinUrl = useScreenStore((state) => state.joinUrl);
  const players = useScreenStore((state) => state.players);
  const playerCount = useScreenStore((state) => state.playerCount);
  const topic = useScreenStore(screenTopic);
  const connected = useScreenStore((state) => state.connected);
  const receive = useScreenStore((state) => state.receive);
  const refused = useScreenStore((state) => state.refused);

  useStomp({
    // An ended game's key is revoked: disconnect rather than retry with it
    credentials: key === null || view === "ended" ? null : { projectorKey: key },
    destinations: topic === null ? [] : [topic],
    onConnected: connected,
    onMessage: (_destination, message) => {
      if (isScreenMessage(message)) {
        receive(message);
      }
    },
    onRefused: refused,
    createClient,
  });

  switch (view) {
    case "connecting":
    case "gettingReady":
      return <GettingReadyView />;
    case "lobby":
      return joinUrl === null ? (
        <GettingReadyView />
      ) : (
        <LobbyView joinUrl={joinUrl} players={players} playerCount={playerCount} />
      );
    case "round":
      // TODO(US-39 and later): the practice, countdown, wall, reveal and winner views (S-03 to S-11)
      return <ProjectorShell title={copy.brand} />;
    case "ended":
      return <EndedView message={endedMessage ?? copy.screen.finished} />;
    default:
      return assertNever(view);
  }
}
