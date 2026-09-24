---
paths:
  - "frontend/**"
---

# Frontend rules (Next.js 16 static export, React 19, TypeScript)

Full rules: document 13, section 7; design: LLD section 6; screens and copy: document 12.

- Strict TypeScript with `noUncheckedIndexedAccess`; no `any` (use `unknown` and narrow). Messages are discriminated unions on `type`, handled by an exhaustive `switch` ending in `assertNever`.
- `Date.now()` only in `src/time` (countdowns use `serverNow()`), `fetch` only in `src/api/http.ts`, and STOMP only through `src/realtime`.
- Every string users see lives in `src/copy.ts`, worded exactly as document 12's copy deck.
- One Zustand store per surface (`player`, `screen`, `admin`). Stores change only through pure functions that apply a server message, tested with the fixtures in `contracts/`. Components read through selectors; derived values are computed, not stored with `useEffect`; every subscription and timer is cleaned up.
- No `style` prop, no `dangerouslySetInnerHTML`, `eval` or `new Function`, and no raw hex colors: use the tokens defined with `@theme` in `globals.css`. The site must work under the hash-based content security policy.
- Static export only: routes take query parameters, such as `/join/?code=K7PQ2M`; no API routes, server actions or dynamic segments.
- No requests to other sites at runtime: no analytics, and no hosted fonts, scripts or images (NFR-24). Press Start 2P is self-hosted, for display text of 16 px or larger only.
- Accessibility: real `<button>` and `<a>` elements; every icon is decorative beside text or has an `aria-label`; announcements go through the shared `LiveAnnouncer`; each new screen gets an axe check in its Playwright test.
- Components in PascalCase, one per file, named exports; other modules in camelCase; hooks start with `use`.
- Before a pull request: `npm run format:check && npm run lint && npm run typecheck && npm test -- --coverage && npm run build`.
