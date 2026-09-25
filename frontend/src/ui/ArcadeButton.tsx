import type { ButtonHTMLAttributes } from "react";

export type ArcadeButtonVariant = "primary" | "secondary" | "danger";

type ArcadeButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ArcadeButtonVariant;
};

// Arcade frames: 2 px borders, square corners, no shadows (document 12, section 5.4). Danger buttons fill with
// --danger-bg, because text on --danger fails contrast (section 5.2).
const variantClasses: Record<ArcadeButtonVariant, string> = {
  primary: "border-primary bg-primary text-ink",
  secondary: "border-border bg-surface text-text hover:bg-surface-2",
  danger: "border-danger bg-danger-bg text-text",
};

// Primary, secondary and danger buttons (document 12, section 5.7), at least 48 px tall (UX-04)
export function ArcadeButton({
  variant = "primary",
  type = "button",
  className = "",
  ...props
}: ArcadeButtonProps) {
  return (
    <button
      type={type}
      className={`inline-flex min-h-12 items-center justify-center gap-2 rounded-none border-2 px-4 py-2 font-sans text-base font-semibold disabled:cursor-not-allowed disabled:opacity-60 ${variantClasses[variant]} ${className}`}
      {...props}
    />
  );
}
