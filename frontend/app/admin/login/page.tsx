"use client";

import { useRouter } from "next/navigation";
import { LoginScreen } from "@/admin/screens/LoginScreen";

// A-01 Login (document 12, section 9): after login, the admin panel opens at A-02 Home
export default function AdminLoginPage() {
  const router = useRouter();
  return <LoginScreen onLoggedIn={() => router.replace("/admin/")} />;
}
