// The player token in local storage, one per game as dh.token.<CODE> (FR-007, document 11 section 7.2). Storage can
// be missing or blocked, for example in a private window, so every access is guarded and failure means "no token".

function key(code: string): string {
  return `dh.token.${code}`;
}

export function saveToken(code: string, token: string): void {
  try {
    window.localStorage.setItem(key(code), token);
  } catch {
    // Without storage, the phone stays joined until the tab closes
  }
}

export function loadToken(code: string): string | null {
  try {
    return window.localStorage.getItem(key(code));
  } catch {
    return null;
  }
}
