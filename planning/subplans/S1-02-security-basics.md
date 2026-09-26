# S1-02 Security basics: headers, CSP, CSRF, rate limits

| Field | Value |
|---|---|
| Status | In progress |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | EN-06 |
| Priority and points | Must, 3 |
| Depends on | S0-01 |
| Unblocks | S1-03, S1-18 |
| Target dates | Wed 30 Sep |
| Branch | feat/en-06-security-basics |
| Parallel-safe with | S1-01, S1-05 |

## Goal

Every page and API response carries the security headers and a hash-based content security policy, state-changing admin requests need a CSRF token, joins and answers are rate-limited, and markup in content is shown as literal text.

## Sources

- Document 04: EN-06; document 05: AC-EN06-01 to AC-EN06-04
- SRS: NFR-13 (HTTPS, EN-02), NFR-16, NFR-17, NFR-19, NFR-20
- LLD: section 5.9 (filter chain, CSRF, client IP, `RateLimiter`), 5.6 (`RealtimeController` answer rate limit), 6.2 (`http.ts`), 6.6 (`csp-hashes.mjs`)
- API: section 5 (5.2 CSRF, 5.3 rate limits); DI-18 (rate-limited answers get no reply; logout needs CSRF)
- Document 16: Appendix B.4 and B.5 (Nginx template and routes)
- Charter: DEC-108 (rate limits), DEC-132 (security design), DEC-135 (CSP hashes)
- Document 15: section 9 (E2E-07), section 6 (DS-05)

## Context to load

- `node planning/scripts/run.mjs section 08 5.9`
- `node planning/scripts/run.mjs section 08 6.6`
- `node planning/scripts/run.mjs section 11 5`
- `node planning/scripts/run.mjs section 16 B.4`
- `node planning/scripts/run.mjs section 16 B.5`
- `node planning/scripts/run.mjs section 15 "E2E-07"`
- `node planning/scripts/run.mjs section 05 EN-06`
- `node planning/scripts/run.mjs section 08 5.6`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-EN06-01 | TC-EN06-01 | End-to-end | `security-privacy` |
| AC-EN06-02 | TC-EN06-02 | Integration | `SecurityIT` |
| AC-EN06-03 | TC-EN06-03 | Integration | `SecurityIT` |
| AC-EN06-04 | TC-EN06-04 | End-to-end | `security-privacy` |

## Tasks

- [x] T1 `SecurityConfig` filter chain as LLD 5.9 lists it (login open and rate-limited, `/api/admin/**` authenticated, `/api/games/**` open, `/api/ops/**` and `/actuator/health`, `/ws` open at HTTP level, anything else denied), plus the header set on API responses, in `app.deliveryhero.security`, test first: `SecurityIT` route and header checks, source: AC-EN06-01, NFR-20 (shared), DEC-132, LLD 5.9
- [x] T2 CSRF: cookie-based token for the single-page app on POST, PUT, PATCH and DELETE under `/api/admin/**`, logout included; `frontend/src/api/http.ts` reads `XSRF-TOKEN` and sends `X-XSRF-TOKEN`, in `app.deliveryhero.security` and `frontend/src/api`, test first: `SecurityIT` AC-EN06-02 (a state-changing request without a valid token is rejected and nothing changes), source: AC-EN06-02, NFR-16, API 5.2, LLD 6.2
- [x] T3 `RateLimiter` fixed windows and `RateLimitFilter` for `join:<ip>` at 120 per minute, 429 `RATE_LIMITED` as Problem Details, and the client IP taken from `X-Forwarded-For` only from the Nginx container (`server.forward-headers-strategy=native`), in `app.deliveryhero.security`, test first: `SecurityIT` AC-EN06-03 (the 121st join from one IP is refused; another IP still joins), source: AC-EN06-03, NFR-17, DEC-108, API 5.3, LLD 5.9
- [ ] T4 Answer rate limit `answer:<playerId>` at 5 per second in `RealtimeController`'s answer mapping: stamp `receivedAt` from the clock first, then drop the excess silently with no reply, in `app.deliveryhero.realtime` and `app.deliveryhero.security`, test first: `SecurityIT` AC-EN06-03 (the 6th answer in a second is dropped; another player is unaffected), source: AC-EN06-03, NFR-17, LLD 5.6 and 5.9, API 5.3
- [ ] T5 Nginx headers on every page: the content security policy include, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer` and `frame-ancestors 'none'`, matching document 16's copies, in `deploy/nginx/templates/delivery-hero.conf.template` and `deploy/nginx/snippets/routes.conf`, test first: `security-privacy` step 1 AC-EN06-01, source: AC-EN06-01, NFR-19 (shared), NFR-20 (shared), E2E-07, document 16 Appendix B.4 and B.5
- [ ] T6 `frontend/scripts/csp-hashes.mjs` after `next build`: SHA-256 hashes of inline scripts without `src`, written to the Nginx CSP file with the directives of LLD 6.6, wired into the frontend build, test first: `security-privacy` page load that fails on any CSP console violation, source: AC-EN06-01, NFR-19 (shared), DEC-135, LLD 6.6
- [ ] T7 Content is rendered as text only (no `dangerouslySetInnerHTML` anywhere; an ESLint rule forbids it), with the DS-05 strings as a shared test fixture, in `frontend/src/ui` and `frontend/e2e/fixtures`, test first: `CodeBlock.test.tsx` and `SpeechBubble.test.tsx` render DS-05 as literal text, source: AC-EN06-04, NFR-19 (shared), DS-05
- [ ] T8 `security-privacy` spec steps 1 and 3 on the local e2e stack: headers on every page and API response, and every request goes to the game's own address, in `frontend/e2e/security-privacy.spec.ts`, test first: the spec itself, source: E2E-07, AC-EN06-01, AC-EN08-02 (shared)
- [ ] T9 `security-privacy` step 2: DS-05 loaded into the e2e stack, the markup shown as literal text on a phone through a practice task, on the projector and in the admin panel, with no script run and no CSP violation, test first: the spec's step 2, source: AC-EN06-04, E2E-07, DS-05 [Blocked: waiting for S2-15 practice round and S2-07 task library]

## Owner actions

None.

## Verification

- `/check` (backend verify, frontend format, lint, typecheck, tests and build)
- `/e2e` for `security-privacy`
- `curl -sI http://localhost:8080/` and `curl -sI http://localhost:8080/api/games/XXXXXX` on the local stack show the four headers
- On production after the first deploy (Q-01): the same `curl -sI` against the site, as OPS-04 (run by S0-06)

## Risks and open questions

- E2E-07 step 2 goes through a practice task (US-10, S2-15) and the admin task library (US-51, S2-07). T9 stays blocked until they exist; T7 proves the rendering rule at component level now. Until T9 passes, AC-EN06-04 isn't fully met, and CP-S1 should list it as carried over.
- DI-18: answers over the rate limit get no reply (API 5.3 wins over 8.4), and logout needs CSRF; built that way here.
- LLD digest note: section 6.6 names `nginx/csp.conf` without a full path. Put it next to the Nginx files in `deploy/nginx/` and check it against document 16, Appendix B.4, before writing.
- DI-19: rate-limit logs carry the IP and the limit only, never names, answers, tokens or keys.
- Changes under `deploy/` need `ops-reviewer`; don't touch the deploy lock or the deploy script.

## Definition of done

Document 13, section 10, plus: `SecurityIT` covers CSRF and both rate limits, `security-privacy` steps 1 and 3 pass locally with no CSP violation, and T9 is either passing or carried over in the CP-S1 notes.

## Claude Code playbook

- `/dh`, then `/story` for EN-06; plan mode (security work).
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `ops-reviewer` (Nginx files), `spec-guardian`.
- Pitfalls: stamp `receivedAt` before rate-limiting; login handlers return 204 or 401, never redirects; no `unsafe-inline` for scripts; the CSRF cookie must be readable by the frontend but the session cookie must stay HttpOnly; no fixed sleeps in the rate-limit tests (inject the clock).

## Progress log

- 2026-09-26: T1 done. The open routes (health, `/api/ops/**`, `/api/games/**`) now allow every method, so `HEAD /health` stops returning 401. API responses carry `Referrer-Policy: no-referrer` and `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'` beside Spring's `nosniff`; the dev API documentation keeps its scripts. `SecurityIT` has 4 tests passing.
- 2026-09-26: T2 done. `csrf.spa()` sets the readable `XSRF-TOKEN` cookie and accepts it back as `X-XSRF-TOKEN`; public joins stay exempt. `SecurityIT` proves it through a test-only admin endpoint (missing, mismatched and valid tokens) until the admin API has its own state-changing requests. The spring-security-test `csrf()` helper swaps the shared filter's repository for one that writes no cookie, so the tests send real cookies and headers instead. `http.ts` already sent the header, covered by `http.test.ts`.
- 2026-09-26: T3 done. `RateLimiter` keeps fixed windows that start at a key's first request (a burst never straddles two windows), with `JOIN` (120 a minute) and `ANSWER` (5 a second), and drops expired windows above 10,000 keys. `RateLimitFilter` runs before the CSRF filter and hands 429 `RATE_LIMITED` to `ProblemHandler`, so the body matches every other refusal; its log line carries the limit and the IP only (DI-19). `server.forward-headers-strategy=native` was already set. Tests: `RateLimiterTest` (4) and `SecurityIT` AC-EN06-03 (121st join refused, another IP joins).
