import { copy } from "@/copy";
import { AdminShell } from "@/admin/components/AdminShell";

// A-08 New game and A-09 Live control (document 12, section 9)
// TODO(US-59): build the screen from document 12
export default function GamesPage() {
  return <AdminShell title={copy.admin.newGame} />;
}
