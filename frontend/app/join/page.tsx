import { Suspense } from "react";
import { PlayerApp } from "@/player/PlayerApp";

// The player app; reads ?code= (LLD section 6.1). Suspense lets the static export pre-render around the
// search-parameter read.
export default function JoinPage() {
  return (
    <Suspense>
      <PlayerApp />
    </Suspense>
  );
}
