"use client";

import { useSearchParams } from "next/navigation";
import { useEffect } from "react";
import { copy } from "@/copy";
import { PhoneShell } from "@/player/PhoneShell";
import { Countdown } from "@/player/screens/Countdown";
import { JoinScreen } from "@/player/screens/JoinScreen";
import { Lobby } from "@/player/screens/Lobby";
import { Practice } from "@/player/screens/Practice";
import { saveToken } from "@/player/session";
import { usePlayerStore } from "@/player/store";
import { useStomp } from "@/realtime/useStomp";
import type { JoinResponse } from "@/types/dto";
import { isPlayerMessage } from "@/types/messages";

const PLAYER_DESTINATIONS = ["/user/queue/game"] as const;

// The phone app's entry point (LLD section 6.3): the join form, then the screen the server's GAME_STATE selects.
// TODO(US-38): the Chrome check (P-01); TODO(US-05): restoring a saved token, the reconnect banner and a refused token
export function PlayerApp() {
  const code = useSearchParams().get("code");
  const screen = usePlayerStore((state) => state.screen);
  const name = usePlayerStore((state) => state.name);
  const token = usePlayerStore((state) => state.token);
  const reset = usePlayerStore((state) => state.reset);
  const joined = usePlayerStore((state) => state.joined);
  const receive = usePlayerStore((state) => state.receive);
  const setConnection = usePlayerStore((state) => state.setConnection);

  useEffect(() => {
    reset(code ?? "");
  }, [code, reset]);

  const connection = useStomp({
    credentials: token === undefined ? null : { playerToken: token },
    destinations: PLAYER_DESTINATIONS,
    onMessage: (_destination, message) => {
      if (isPlayerMessage(message)) {
        receive(message);
      }
    },
  });
  useEffect(() => {
    setConnection(connection);
  }, [connection, setConnection]);

  const onJoined = (result: JoinResponse) => {
    if (code !== null) {
      saveToken(code, result.token);
    }
    joined(result);
  };

  switch (screen) {
    case "checking":
    case "notice":
    case "joinForm":
    case "restoring":
      return <JoinScreen code={code} onJoined={onJoined} />;
    case "lobby":
      return <Lobby name={name ?? ""} />;
    case "practice":
      return <Practice />;
    case "countdown":
      return <Countdown />;
    default:
      // TODO(US-16 and later): the task, feedback, incident, done, time's up, results and ending screens
      return <PhoneShell title={copy.brand} />;
  }
}
