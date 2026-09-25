import { copy } from "@/copy";
import { AdminShell } from "@/admin/components/AdminShell";

// A-02 Home (document 12, section 9)
// TODO(US-59): build the screen from document 12
export default function AdminHomePage() {
  return <AdminShell title={copy.admin.brand} />;
}
