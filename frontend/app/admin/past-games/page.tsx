import { copy } from "@/copy";
import { AdminShell } from "@/admin/components/AdminShell";

// A-10 Past games (document 12, section 9)
// TODO(US-64): build the screen from document 12
export default function PastGamesPage() {
  return <AdminShell title={copy.admin.nav.pastGames} />;
}
