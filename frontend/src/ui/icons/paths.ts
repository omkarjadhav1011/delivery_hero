// Interface icons from Pixelarticons 2.4.1 by Gerrit Halfmann, MIT license (./LICENSE; DEC-168, UX-03). Only the
// path data of the icons in document 12, section 5.5 is copied, so they draw in the current text color.
export const iconPaths = {
  check: [
    "M10 18H8v-2h2v2Zm-2-2H6v-2h2v2Zm4-2v2h-2v-2h2Zm-6 0H4v-2h2v2Zm8 0h-2v-2h2v2Zm2-2h-2v-2h2v2Zm2-2h-2V8h2v2Zm2-2h-2V6h2v2Z",
  ],
  // Pixelarticons "close"
  cross: [
    "M7 19H5V17H7V19ZM19 19H17V17H19V19ZM9 15V17H7V15H9ZM17 17H15V15H17V17ZM11 15H9V13H11V15ZM15 15H13V13H15V15ZM13 13H11V11H13V13ZM11 11H9V9H11V11ZM15 11H13V9H15V11ZM9 9H7V7H9V9ZM17 9H15V7H17V9ZM7 7H5V5H7V7ZM19 7H17V5H19V7Z",
  ],
  lock: ["M5 8h14v2H5zm0 12h14v2H5zM3 10h2v10H3zm16 0h2v10h-2zM7 4h2v4H7zm2-2h6v2H9zm6 2h2v4h-2z"],
  // Pixelarticons "fire"
  flame: [
    "M9 2h2v4H9zM7 6h2v2H7zM5 8h2v2H5zm8 2h2v2h-2zm2-2h2v2h-2zm2 2h2v2h-2zm2 2h2v6h-2zM3 10h2v8H3zm8-4h2v4h-2zm6 12h2v2h-2zM7 20h10v2H7zm-2-2h2v2H5zm4-2h6v4H9z",
    "M11 14h2v3h-2z",
  ],
  // Pixelarticons "cellular-signal-0"
  noSignal: [
    "M4 14h2v2H4zm7-4h2v2h-2zm7-6h2v2h-2zM2 16h2v2H2zm7-4h2v6H9zm7-6h2v12h-2zM6 14h2v4H6zm7-4h2v8h-2zm7-6h2v14h-2zM2 18h6v2H2zm7 0h6v2H9zm7 0h6v2h-6z",
  ],
  siren: [
    "M6 11h2v5H6zm2-2h2v2H8zm2-2h4v2h-4zm4 2h2v2h-2zm2 2h2v5h-2zM6 16h12v2H6zm-2 4h16v2H4zm0-2h2v2H4zm14 0h2v2h-2zm-7-6h2v4h-2zm9-1h3v2h-3zM1 11h3v2H1zm5-6h2v2H6zm10 0h2v2h-2zm2-2h2v2h-2zM4 3h2v2H4zm7-1h2v3h-2z",
  ],
  clock: [
    "M6 2h12v2H6zM2 6h2v12H2zm18 0h2v12h-2zm-2-2h2v2h-2zM4 4h2v2H4zm2 18h12v-2H6zm12-2h2v-2h-2zM4 20h2v-2H4zm7-14h2v7h-2zm2 7h2v2h-2zm2 2h2v2h-2z",
  ],
} as const satisfies Record<string, readonly string[]>;

export type IconName = keyof typeof iconPaths;

export const iconNames = Object.keys(iconPaths) as IconName[];
