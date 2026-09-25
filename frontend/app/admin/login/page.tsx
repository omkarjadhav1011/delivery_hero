import { copy } from "@/copy";
import { AdminShell } from "@/admin/components/AdminShell";

// A-01 Login (document 12, section 9)
// TODO(US-49): build the screen from document 12
export default function AdminLoginPage() {
  return <AdminShell title={copy.admin.login.heading} navigation={false} />;
}
