import { copy } from "@/copy";
import { PhoneShell } from "@/player/PhoneShell";

// The site's root: players arrive through the join link or QR code instead (LLD section 6.1)
export default function HomePage() {
  return <PhoneShell title={copy.home.title} />;
}
