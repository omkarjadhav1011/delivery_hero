/** Ends an exhaustive switch: the compiler flags any case left unhandled (document 13, section 7). */
export function assertNever(value: never): never {
  throw new Error(`Unhandled value: ${String(value)}`);
}
