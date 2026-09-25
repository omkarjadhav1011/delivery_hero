import type { ReactNode } from "react";

export type BadgeTone = "primary" | "accent" | "success" | "warning";

// Dark ink on a bright fill, which passes contrast for every tone (document 12, section 5.2)
const toneClasses: Record<BadgeTone, string> = {
  primary: "bg-primary",
  accent: "bg-accent",
  success: "bg-success",
  warning: "bg-warning",
};

// A small label, such as the streak's ×1.5 badge (document 12, section 5.7)
export function Badge({ tone = "primary", children }: { tone?: BadgeTone; children: ReactNode }) {
  return (
    <span
      className={`inline-flex items-center gap-1 px-2 py-0.5 font-sans text-sm font-bold text-ink ${toneClasses[tone]}`}
    >
      {children}
    </span>
  );
}
