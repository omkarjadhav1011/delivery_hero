import type { ReactNode } from "react";
import { copy } from "@/copy";

type ProjectorShellProps = {
  title: string;
  // The 4-column sidebar beside the 8-column wall (document 12, section 5.4)
  sidebar?: ReactNode;
  children?: ReactNode;
};

// Every projector screen's frame (document 12, sections 5.4 and 6.2): a 16:9 page for 1920 × 1080 and
// 1280 × 720, sized in rem so it reads from the back of the room, with a 12-column grid.
export function ProjectorShell({ title, sidebar, children }: ProjectorShellProps) {
  return (
    <div className="flex h-dvh flex-col gap-6 overflow-hidden p-8">
      <header className="flex items-center justify-between">
        <p className="font-display text-4xl text-accent">{copy.brand}</p>
      </header>
      <main className="grid min-h-0 flex-1 grid-cols-12 gap-6">
        <div className={`${sidebar ? "col-span-8" : "col-span-12"} flex min-w-0 flex-col gap-6`}>
          <h1 className="text-4xl font-semibold">{title}</h1>
          {children}
        </div>
        {sidebar && <aside className="col-span-4 min-w-0">{sidebar}</aside>}
      </main>
    </div>
  );
}
