import { copy } from "@/copy";
import { AdminShell } from "@/admin/components/AdminShell";

// A-07 Run plan editor, reads ?id= (document 12, section 9)
// TODO(US-57): build the screen from document 12
export default function RunPlanEditorPage() {
  return <AdminShell title={copy.admin.nav.runPlans} />;
}
