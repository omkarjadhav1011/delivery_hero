# S1-03 Admin login and attempt limit

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | US-49, US-50 |
| Priority and points | Must, 4 |
| Depends on | S1-02 |
| Unblocks | S1-04, S2-07, S2-09, S2-24, S1-18 |
| Target dates | Thu 1 Oct |
| Branch | feat/us-49-admin-login |
| Parallel-safe with | S1-01, S1-05 |

## Goal

Admins log in with the shared password (checked against a bcrypt hash), get a 12-hour session cookie with the right flags, can log out, and one IP address is blocked for 15 minutes after 5 failed logins.

## Sources

- Document 04: US-49, US-50; document 05: AC-US49-01 to AC-US49-05, AC-US50-01 to AC-US50-03
- SRS: FR-067, FR-068, NFR-14, NFR-15
- LLD: section 5.9 (login, session, `RateLimiter` login window), 5.13 (`dh.admin.password-hash`, session timeout), 5.14 (`LOGIN_SUCCEEDED`, `LOGIN_FAILED`, `RATE_LIMITED`)
- API: section 7.3 (admin session endpoints), 5.3 (rate limits)
- Document 12: A-01 (Login) and the copy deck
- Charter: DEC-97 (session), DEC-98 (password hash), DEC-104 (logs), R-05 (shared admin password)
- Document 15: section 12 (MAN-08)

## Context to load

- `node planning/scripts/run.mjs section 08 5.9`
- `node planning/scripts/run.mjs section 11 7.3`
- `node planning/scripts/run.mjs section 11 5.3`
- `node planning/scripts/run.mjs section 12 A-01`
- `node planning/scripts/run.mjs section 08 5.13`
- `node planning/scripts/run.mjs section 05 US-49`
- `node planning/scripts/run.mjs section 05 US-50`
- `node planning/scripts/run.mjs section 15 12`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US49-01 | TC-US49-01 | End-to-end | `content-admin` |
| AC-US49-02 | TC-US49-02 | Integration | `SecurityIT` |
| AC-US49-03 | TC-US49-03 | Integration | `SecurityIT` |
| AC-US49-04 | TC-US49-04 | Integration | `SecurityIT` |
| AC-US49-05 | TC-US49-05 | Manual | MAN-08 |
| AC-US50-01 | TC-US50-01 | Integration | `SecurityIT` |
| AC-US50-02 | TC-US50-02 | Integration | `SecurityIT` |
| AC-US50-03 | TC-US50-03 | Integration | `SecurityIT` |

## Tasks

- [x] T1 `LoginHandlers`: form login at `POST /api/admin/login` for the single user `admin`, bcrypt check against `dh.admin.password-hash` (from `DH_ADMIN_PASSWORD_HASH`), 204 or 401 `UNAUTHENTICATED` with no redirects; `GET /api/admin/session`, in `app.deliveryhero.security`, test first: `SecurityIT` login returns 204 with the right password and 401 with a wrong one, source: AC-US49-01, FR-067, DEC-98, API 7.3, LLD 5.9
- [x] T2 Session: `server.servlet.session.timeout=12h`, `DH_SESSION` cookie HttpOnly, Secure and SameSite=Strict, and `POST /api/admin/logout` ending the session, in `app.deliveryhero.security` and `backend/src/main/resources/application.yml`, test first: `SecurityIT` AC-US49-02 (action at 11 h 59 min works, at 12 h 01 min asks to log in), AC-US49-03 (old cookie refused after logout), AC-US49-04 (cookie flags), source: AC-US49-02, AC-US49-03, AC-US49-04, NFR-15, DEC-97, LLD 5.9 and 5.13
- [x] T3 Login attempt limit: `login:<ip>` blocks after 5 failures in 15 minutes for 15 minutes, refusing even the right password with 429 `RATE_LIMITED`; other addresses unaffected; the clock is injected, in `app.deliveryhero.security` (`RateLimiter`, `RateLimitFilter`), test first: `SecurityIT` AC-US50-01, AC-US50-02, AC-US50-03, source: AC-US50-01, AC-US50-02, AC-US50-03, FR-068, DEC-108 (shared), API 5.3, R-05
- [x] T4 Login and security logs: `LOGIN_SUCCEEDED`, `LOGIN_FAILED` and `RATE_LIMITED` carry the IP and limit only; the password never reaches a log line, in `app.deliveryhero.security`, test first: `SecurityIT` AC-US49-05 log capture during a failed and a successful login finds no password, source: AC-US49-05, NFR-14 (shared), DEC-104 (shared), LLD 5.14
- [ ] T5 Login screen: password field, "Log in", the failure and rate-limit messages from `src/copy.ts`, and a redirect to login on `UNAUTHENTICATED` from any admin call, in `frontend/app/admin/login/page.tsx`, `frontend/src/admin` and `frontend/src/api/http.ts`, test first: `LoginScreen.test.tsx`, source: AC-US49-01, FR-067, FR-068, document 12 Login screen and copy deck
- [ ] T6 The login step of the `content-admin` spec (E2E-04 step 1) on the local e2e stack with the local password, so later admin specs reuse it, in `frontend/e2e/content-admin.spec.ts` and a shared login fixture, test first: the spec's login step, source: AC-US49-01, E2E-04 (shared)
- [ ] T7 Owner: MAN-08 on production: inspect `/opt/delivery-hero/.env` for a bcrypt hash with cost 12 or more and search the logs for the password; record the result, test first: none, source: MAN-08, AC-US49-05, NFR-14 (shared), R-05, document 15 section 12 [Blocked: waiting for Q-01]

## Owner actions

- OA-15: choose the admin password and put its bcrypt hash (cost 12) in `.env` (needed for T7; waits on the production host, Q-01).

## Verification

- `/check` (backend verify, frontend checks and tests)
- `/e2e` for the `content-admin` login step
- Local: log in at <http://localhost:8080/admin/login> with `delivery-hero-local`, then `curl -si -X POST http://localhost:8080/api/admin/login` six times with a wrong password; the sixth gets 429
- MAN-08 on production once Q-01 is answered

## Risks and open questions

- R-05: a shared password with no audit trail. Handled as the Charter says: bcrypt hash only (NFR-14), rate-limited login (FR-068), HTTPS only (Secure cookie). Changing the password when someone leaves the admin group is an owner procedure in document 16.
- LLD digest note: the 15-minute login window has no configuration key in LLD 5.13. Keep it a constant next to the `dh.rate.login-failures` setting; don't invent a new key without asking.
- DI-18: logout needs CSRF like every state-changing admin request (S1-02 builds the token).
- DI-04 / Q-01: MAN-08 on production waits for a host; the local stack can be checked now the same way.
- AC-US49-01's primary test is E2E-04 (shared, owned by S2-07); T6 writes only its login step here.

## Definition of done

Document 13, section 10, plus: `SecurityIT` covers every US-49 and US-50 integration criterion with an injected clock, the login step of `content-admin` passes locally, and T7 is recorded as blocked or passed.

## Claude Code playbook

- `/dh`, then `/story` for US-49 (US-50 rides along); plan mode (security work).
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian`.
- Pitfalls: no redirects from login handlers; never log the password or the hash; test the 12-hour expiry with a controllable clock, not by waiting; the local password lives only in the local stack's files; the admin password field must have an accessible label.

## Progress log

- 2026-09-26: T1 done. Form login at `POST /api/admin/login` answers 204 or 401 `UNAUTHENTICATED` (new in `ApiErrorCode`, no detail: A-01 words its own message) with no redirect; `LoginHandlers` is also the entry point, so a call without a session gets the same Problem Details. `GET /api/admin/session` is open and loads the CSRF token, because login needs it (DI-51). Per LLD 5.9 only `/api/admin/**` is authenticated and other API paths are denied (owner choice). The request cache is off, so refused calls create no session. `AdminSession` keeps a fixed end from login (used by T2). `SecurityIT`: 12 pass.
- 2026-09-26: T2 done. The session cookie is `DH_SESSION`, HttpOnly, Secure and SameSite=Strict (`application.yml`). The servlet timeout counts idle time, so `AdminSession` stores a fixed end, login time plus `server.servlet.session.timeout` from the injected clock, and `AdminSessionExpiryFilter` ends the session after it (AC-US49-02); the 12 h idle timeout stays as the backstop. Logout at `POST /api/admin/logout` needs CSRF (DI-18) and answers 204. AC-US49-03 and AC-US49-04 are in `AdminSessionIT` (a real server, since MockMvc never produces Tomcat's cookie), not `SecurityIT`; recorded for document 15. Tests: `SecurityIT` 13, `AdminSessionIT` 2.
- 2026-09-26: T3 done. `RateLimiter.LOGIN` (5 failures in 15 minutes) and `LOGIN_BLOCK` (15 minutes) are constants beside JOIN and ANSWER (owner choice; the count is `dh.rate.login-failures` in LLD 5.13, recorded as a doc issue). The failure handler counts each failed login under `login:<ip>`; the one that reaches 5 blocks the address. `RateLimitFilter` refuses a blocked address's login, even with the right password, with 429 `RATE_LIMITED` and `Retry-After` in whole seconds. A success doesn't reset the count (the documents don't say it should). Tests: `SecurityIT` AC-US50-01 to 03 (16 pass), `RateLimiterTest` 7.
- 2026-09-26: T4 done. `LoginHandlers` logs `LOGIN_SUCCEEDED` (INFO) and `LOGIN_FAILED` (WARN) with the IP only, and `RateLimitFilter` logs `RATE_LIMITED` with `limit` (`join` or `login`) and the IP (LLD 5.14). `SecurityIT` AC-US49-05 captures the output of a failed and a successful login: the events and the address are there, and the password, the wrong guess and the hash are not. `GET /api/admin/session` now reads the CSRF token from the request, so the OpenAPI document shows no `csrf` parameter. `./mvnw -B verify`: everything passes except `OpenApiIT`, which waits for the owner to approve updating `docs/openapi.json` with the new endpoint.
