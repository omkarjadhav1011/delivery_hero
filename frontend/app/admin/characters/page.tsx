import { copy } from "@/copy";
import { AdminShell } from "@/admin/components/AdminShell";

// A-05 Characters (document 12, section 9)
// TODO(US-55): build the screen from document 12
export default function CharactersPage() {
  return <AdminShell title={copy.admin.nav.characters} />;
}
