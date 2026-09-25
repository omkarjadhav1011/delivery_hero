import type { ReactNode } from "react";
import { copy } from "@/copy";

type PhoneShellProps = {
  title: string;
  // The in-game top bar (time, points, streak); the brand until then. TODO(US-16): TopBar
  topBar?: ReactNode;
  // Controls kept at the bottom, within thumb reach (document 12, section 5.4)
  actions?: ReactNode;
  children?: ReactNode;
};

// Every phone screen's frame (document 12, sections 5.4 and 6.1): the top bar area, the content and the bottom
// actions. It stretches to the phone's width with no fixed widths, so 320 px and 200% text never scroll sideways
// (NFR-30), and keeps inside the safe area.
export function PhoneShell({ title, topBar, actions, children }: PhoneShellProps) {
  return (
    <div className="mx-auto flex min-h-dvh max-w-md min-w-0 flex-col px-4 pt-[max(1rem,env(safe-area-inset-top))] pb-[max(1rem,env(safe-area-inset-bottom))] break-words">
      <header className="flex min-h-12 items-center justify-center">
        {topBar ?? <p className="font-display text-base text-accent">{copy.brand}</p>}
      </header>
      <main className="flex flex-1 flex-col items-center justify-center gap-6 py-4 text-center">
        <h1 className="text-xl font-semibold">{title}</h1>
        {children}
      </main>
      {actions && (
        <div data-phone-actions className="flex flex-col gap-3">
          {actions}
        </div>
      )}
    </div>
  );
}
