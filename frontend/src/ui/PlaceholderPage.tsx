import type { ReactNode } from "react";
import { copy } from "@/copy";

type PlaceholderPageProps = {
  title: string;
  children?: ReactNode;
};

// A page shell for the scaffold (EN-01): the brand and the page's title. Each screen replaces it with its
// real layout from document 12 in its own story.
export function PlaceholderPage({ title, children }: PlaceholderPageProps) {
  return (
    <main className="mx-auto flex min-h-screen max-w-3xl flex-col items-center justify-center gap-6 p-4 text-center">
      <p className="font-display text-base text-accent">{copy.brand}</p>
      <h1 className="text-xl">{title}</h1>
      {children}
    </main>
  );
}
