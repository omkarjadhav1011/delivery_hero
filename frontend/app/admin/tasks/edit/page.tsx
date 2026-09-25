import { copy } from "@/copy";
import { AdminShell } from "@/admin/components/AdminShell";

// A-04 Task editor, reads ?id= (document 12, section 9)
// TODO(US-51): build the screen from document 12
export default function TaskEditorPage() {
  return <AdminShell title={copy.admin.editTask} />;
}
