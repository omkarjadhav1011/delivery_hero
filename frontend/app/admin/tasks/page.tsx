import { copy } from "@/copy";
import { AdminShell } from "@/admin/components/AdminShell";

// A-03 Task library (document 12, section 9)
// TODO(US-52): build the screen from document 12
export default function TaskLibraryPage() {
  return <AdminShell title={copy.admin.nav.tasks} />;
}
