import { copy } from "@/copy";
import { AdminShell } from "@/admin/components/AdminShell";

// A-06 Run plans (document 12, section 9)
// TODO(US-57): build the screen from document 12
export default function RunPlansPage() {
  return <AdminShell title={copy.admin.nav.runPlans} />;
}
