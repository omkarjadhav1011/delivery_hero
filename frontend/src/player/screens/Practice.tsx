import { copy } from "@/copy";
import { PhoneShell } from "@/player/PhoneShell";

// P-05 (document 12), a shell until the practice round exists. TODO(US-10): the practice tasks and "Ready!"
export function Practice() {
  return <PhoneShell title={copy.practice.title} />;
}
