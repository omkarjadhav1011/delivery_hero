import { copy } from "@/copy";
import { PlaceholderPage } from "@/ui/PlaceholderPage";

// The site's root: players arrive through the join link or QR code instead (LLD section 6.1)
export default function HomePage() {
  return <PlaceholderPage title={copy.home.title} />;
}
