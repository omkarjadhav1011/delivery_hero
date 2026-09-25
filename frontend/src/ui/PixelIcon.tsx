import { type IconName, iconPaths } from "./icons/paths";

// An interface icon (UX-03) (document 12, section 5.5). Every icon has an accessible label, or is marked
// decorative because visible text beside it says the same (LLD section 6.7).
type PixelIconProps = { name: IconName; className?: string } & (
  { label: string; decorative?: never } | { decorative: true; label?: never }
);

export function PixelIcon({ name, label, className = "size-6" }: PixelIconProps) {
  const labelled = label !== undefined;
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="currentColor"
      className={`inline-block shrink-0 ${className}`}
      role={labelled ? "img" : undefined}
      aria-label={label}
      aria-hidden={labelled ? undefined : true}
      focusable="false"
    >
      {iconPaths[name].map((d) => (
        <path key={d} d={d} />
      ))}
    </svg>
  );
}
