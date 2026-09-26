"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { adminLogout, getAdminSession } from "@/api/endpoints";
import { setUnauthenticatedHandler } from "@/api/http";
import { copy } from "@/copy";
import { ArcadeButton } from "@/ui/ArcadeButton";

type AdminShellProps = {
  title: string;
  // The login page (A-01) shows no header or navigation, only its heading
  navigation?: boolean;
  children?: ReactNode;
};

// The A-02 header's sections, in its order (document 12, section 9)
const sections = [
  { href: "/admin/tasks/", label: copy.admin.nav.tasks },
  { href: "/admin/characters/", label: copy.admin.nav.characters },
  { href: "/admin/run-plans/", label: copy.admin.nav.runPlans },
  { href: "/admin/games/", label: copy.admin.nav.games },
  { href: "/admin/past-games/", label: copy.admin.nav.pastGames },
] as const;

// Next.js may report the path with or without the trailing slash, so both sides are compared without it
function isCurrent(pathname: string, href: string): boolean {
  const path = pathname.replace(/\/$/, "");
  const base = href.replace(/\/$/, "");
  return path === base || path.startsWith(`${base}/`);
}

// Every admin screen's frame (document 12, sections 6.3 and 9): the header with the navigation, then the content.
// Built for a laptop at 1280 px or more and fully usable with a keyboard (NFR-32).
export function AdminShell({ title, navigation = true, children }: AdminShellProps) {
  const pathname = usePathname();
  const router = useRouter();

  // Any admin call that finds the session ended sends the admin to login (FR-067); not on the login page itself.
  // The session check on load also hands out the current CSRF token, which login rotates (document 11, 5.2).
  useEffect(() => {
    if (!navigation) {
      return undefined;
    }
    let cancelled = false;
    setUnauthenticatedHandler(() => router.replace("/admin/login/"));
    getAdminSession().then(
      (session) => {
        if (!cancelled && !session.authenticated) {
          router.replace("/admin/login/");
        }
      },
      () => {
        // Unreachable server: the next admin call reports it
      },
    );
    return () => {
      cancelled = true;
      setUnauthenticatedHandler(null);
    };
  }, [navigation, router]);

  return (
    <div className="flex min-h-dvh flex-col">
      {navigation && (
        <header className="flex flex-wrap items-center gap-x-8 gap-y-2 border-b-2 border-border bg-surface px-6 py-4">
          <Link href="/admin/" className="font-display text-base text-accent">
            {copy.admin.brand}
          </Link>
          <nav aria-label={copy.admin.navLabel}>
            <ul className="flex flex-wrap gap-x-6 gap-y-2">
              {sections.map(({ href, label }) => {
                const current = isCurrent(pathname, href);
                return (
                  <li key={href}>
                    <Link
                      href={href}
                      aria-current={current ? "page" : undefined}
                      className={`underline-offset-4 hover:underline ${current ? "text-primary underline" : "text-text"}`}
                    >
                      {label}
                    </Link>
                  </li>
                );
              })}
            </ul>
          </nav>
          <ArcadeButton
            variant="secondary"
            className="ml-auto"
            // Only once the session has really ended; a 401 goes to login through the handler above
            onClick={() => void adminLogout().then(() => router.replace("/admin/login/"))}
          >
            {copy.admin.logout}
          </ArcadeButton>
        </header>
      )}
      <main className="flex flex-1 flex-col gap-6 p-6">
        <h1 className="text-2xl font-semibold">{title}</h1>
        {children}
      </main>
    </div>
  );
}
