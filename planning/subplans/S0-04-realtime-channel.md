# S0-04 Real-time channel

| Field | Value |
|---|---|
| Status | In progress |
| Phase | S0 (Thu 24 – Tue 29 Sep) |
| Stories | EN-04 |
| Priority and points | Must, 5 |
| Depends on | S0-01 |
| Unblocks | S0-05, S1-05 |
| Target dates | Sat 26 Sep – Sun 27 Sep |
| Branch | feat/en-04-realtime-channel |
| Parallel-safe with | S0-02, S0-03 |

## Goal

Phones, the projector and admin panels connect to `/ws` over STOMP 1.2 with 10-second heartbeats; valid credentials are accepted and receive the current state once subscribed, anything else is refused with no game data, destination rules are enforced, and the browser client reconnects on the 0.5 s, 1 s, 2 s, then every 2 s schedule with a "Reconnecting…" banner.

## Sources

- Document 04: EN-04; document 05: AC-EN04-01 to AC-EN04-04
- SRS: sections 6.2 and 6.3 (communication interfaces, heartbeats, reconnect schedule, message sizes), NFR-03 (resume within 5 seconds), NFR-18 (tokens hashed, keys revoked), FR-052 (projector display-only)
- LLD: section 5.6 (`WebSocketConfig`, `StompAuthInterceptor`, `DestinationPolicy`, `RealtimeController`, `StompEventListener`), 5.1 (`realtime` package, `TokenService` in `common`), 6.2 (`stompClient.ts`), LD-02 (projector sends only time sync), LD-08
- API: sections 8.1 (connection and refusal), 8.2 (destinations and permissions), 8.3 (envelope)
- HLD: section 11 (interface overview and destination rules)
- Charter, Appendix A: DEC-122 (offline within 20 seconds of the last heartbeat), DEC-127 (simple broker, no SockJS), DEC-133 (credentials in CONNECT headers, channel interceptor), DEC-134 (`@stomp/stompjs`), DEC-146 (full state after subscription), DEC-104 (no tokens or keys in logs)
- Document 15: section 9, E2E-06 `resilience` (shared, S1-16; covers TC-EN04-04 end to end)

## Context to load

- `node planning/scripts/run.mjs section 05 EN-04`
- `node planning/scripts/run.mjs section 08 5.6`
- `node planning/scripts/run.mjs section 08 6.2`
- `node planning/scripts/run.mjs section 11 8.1`
- `node planning/scripts/run.mjs section 11 8.2`
- `node planning/scripts/run.mjs section 11 8.3`
- `node planning/scripts/run.mjs section 07 11`
- `node planning/scripts/run.mjs section 03 6.3`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-EN04-01 | TC-EN04-01 | Integration | `StompConnectionIT` |
| AC-EN04-02 | TC-EN04-02 | Integration | `StompConnectionIT` |
| AC-EN04-03 | TC-EN04-03 | Integration | `StompConnectionIT` |
| AC-EN04-04 | TC-EN04-04 | Frontend | `reconnect.test.ts` |

## Tasks

- [x] T1 `WebSocketConfig`: STOMP endpoint `/ws` limited to the site's own origin, no SockJS; simple broker for `/topic` and `/queue` with 10,000 ms heartbeats both ways on a `ThreadPoolTaskScheduler`; prefixes `/app` and `/user`; 8 KB inbound limit, in `app.deliveryhero.config`, test first: `StompConnectionIT` AC-EN04-03 (a connected idle client stays open for 30 seconds with heartbeats both ways; a client that stops sending heartbeats is closed within 20 seconds, DEC-122), using latches with timeouts, never fixed sleeps, source: AC-EN04-03, SRS 6.3, DEC-127, DEC-122, LLD 5.6
- [x] T2 `TokenService` (hashing and constant-time comparison) and the principals `PlayerPrincipal` (name `p:<playerId>`), `ProjectorPrincipal`, `AdminPrincipal`, in `app.deliveryhero.common` and `app.deliveryhero.realtime`, test first: `TokenServiceTest` (the same token hashes the same; a stored value is never the raw token), source: NFR-18, BR-17, LLD 5.1 and 5.6
- [x] T3 `StompAuthInterceptor` on the client inbound channel: CONNECT with `player-token` looked up by hash in the current game, `projector-key` compared in constant time, an authenticated admin handshake, and anything else refused with an ERROR frame `message: UNAUTHORIZED`; the lookup is a small port in `realtime` that S0-05 (join) and S1-04 (game creation) fill, and the test registers entries directly; nothing logs the token or key, in `app.deliveryhero.realtime`, test first: `StompConnectionIT` AC-EN04-02 (unknown token, revoked token and wrong key refused; no MESSAGE frame received; captured logs contain neither value), source: AC-EN04-02, NFR-18, DEC-133, DEC-104 (shared), API 8.1, LLD 5.6
- [x] T4 `DestinationPolicy.maySubscribe` and `maySend` per HLD section 11 and API 8.2: players only their `/user/queue/game` and `/user/queue/time-sync` and `/app/games/{gameId}/answer`; the projector only its game's `/topic/games/{gameId}/screen` and `/app/time-sync` (LD-02); admins `/topic/games/{gameId}/admin`; every other SUBSCRIBE or SEND refused with an ERROR frame, in `app.deliveryhero.realtime`, test first: `DestinationPolicyTest` (one row per destination and principal) and `StompConnectionIT` AC-EN04-02 (a player subscribing to another game's screen topic is refused), source: AC-EN04-02, FR-052, LD-02, DEC-133, HLD 11, API 8.2
- [x] T5 `StompEventListener` turning connect, subscribe and disconnect events into `Reconnect`, `ClientSubscribed` and `Disconnect` commands, and the full current state sent once a subscription is confirmed (GAME_STATE to a player, SCREEN_STATE to the projector, LIVE_STATS to admins) from a state provider that S0-05 and S1-05 fill, in the server envelope `type` plus `serverTime`, in `app.deliveryhero.realtime` and `app.deliveryhero.engine.command`, test first: `StompConnectionIT` AC-EN04-01 (a valid player token, projector key and admin session each connect and receive their state after subscribing, not before), source: AC-EN04-01, SRS 6.2, DEC-146, API 8.1 and 8.3, LLD 5.6
- [x] T6 Reconnect schedule module (0.5 s, 1 s, 2 s, then every 2 s) driven by an injected timer, in `frontend/src/realtime/reconnect.ts`, test first: `reconnect.test.ts` AC-EN04-04 (with fake timers: a 20-second network drop retries on the schedule, shows "Reconnecting…" after the first failed attempt, and resumes within 5 seconds of the network returning), source: AC-EN04-04, NFR-03, SRS 6.3, LLD 6.2
- [ ] T7 `stompClient.ts` and `useStomp.ts` wrapping `@stomp/stompjs`: `player-token` or `projector-key` in CONNECT headers, `heart-beat: 10000,10000`, the T6 schedule in place of the library's own, connection status for the banner, and re-subscribing after every reconnect; the "Reconnecting…" string in `src/copy.ts`, in `frontend/src/realtime/` and `frontend/src/copy.ts`, test first: `stompClient.test.ts` (headers set; subscriptions restored after a reconnect; status changes) and `reconnect.test.ts` AC-EN04-04, source: AC-EN04-04, DEC-134, NFR-03, API 8.1, LLD 6.2, document 12 P-19
- [ ] T8 Local stack check through Nginx: a STOMP client connects to `ws://localhost:8080/ws` with a test token registered in the `dev` profile only, heartbeats flow, and a wrong origin is refused, in `deploy/docker-compose.local.yml` only if the proxy needs a fix, test first: procedure (local stack; the `/ws` location in `deploy/nginx/snippets/routes.conf` upgrades the connection), source: AC-EN04-01 (shared with S0-05's end-to-end run), DEC-67 (shared), DEC-127, document 16 Appendix B.5

## Owner actions

None.

## Verification

- `/check` (backend `./mvnw -B verify` with Docker for `StompConnectionIT`; frontend lint, types, `npm test -- --coverage`, build).
- The local-stack connection check of T8.
- `python3 tools/ac_coverage.py` lists AC-EN04-01 to AC-EN04-04 as automated and passing.
- E2E-06 (`resilience`, S1-16) later covers TC-EN04-04 end to end; not run here.

## Risks and open questions

- Tokens and projector keys don't exist until S0-05 (join) and S1-04 (game creation). T3 and T5 use a small lookup and state-provider port that those subplans fill; `StompConnectionIT` registers entries directly, and S0-05 re-runs it against the real join.
- The admin case needs an authenticated session before S1-03 builds the login. The test authenticates through the scaffold's security configuration; S1-03 re-runs AC-EN04-01 with the real login.
- AC-EN04-03 takes about 30 seconds of wall time. Use latches with timeouts and the real broker, never fixed sleeps; keep it one test so the suite stays fast.
- DI-19 / DEC-104: interceptor and listener logs carry principal type and game ID only, never tokens, keys or names.
- DI-11: this channel never sends answer data; the state provider sends only what API section 8.5 lists for each state.

## Definition of done

Document 13, section 10, plus: `StompConnectionIT` passes all three backend criteria, `reconnect.test.ts` passes AC-EN04-04, every destination rule has a `DestinationPolicyTest` row, and a client connects through Nginx on the local stack.

## Claude Code playbook

- `/dh` in plan mode (real-time and security work), then `/story` for EN-04; `/check` and `/pr`.
- Reviewers: `backend-reviewer`, `frontend-reviewer`; `spec-guardian` on API sections 8.1 to 8.3 and HLD section 11.
- Pitfalls: no SockJS; no fixed sleeps in real-time tests; `Date.now` only in `src/time` (the reconnect schedule takes an injected timer); refuse unknown frames by default; never log a token or key; the full state goes after the subscription is confirmed, never at CONNECT.

## Progress log

- 2026-09-26: T1 done. `/ws` has a simple broker with 10-second heartbeats on its own scheduler, and `/ws` is open at the HTTP level. `HeartbeatWatchdog` closes connections that go silent (DI-36: the broker alone waits 30 s). `StompConnectionIT` AC-EN04-03 passes in about 30 s, and `./mvnw -B verify` passes (30 unit tests, 20 integration tests).
- 2026-09-26: T2 done. `TokenService` makes 16 random bytes as URL-safe Base64 and stores their SHA-256 hash, with `MessageDigest.isEqual` for comparisons. `SecureRandom` comes from `config.RandomConfig`. There is a sealed `ClientPrincipal` with player (`p:<playerId>`), projector and admin records. `TokenServiceTest` (3 tests) and `ArchitectureTest` pass; the full verify runs with T3.
- 2026-09-26: T3 done. `StompAuthInterceptor` checks CONNECT against the `ConnectionCredentials` port, with an in-memory `CredentialRegistry` that US-01 and US-04 will fill. The admin case uses the handshake's `ROLE_ADMIN` authentication. Refusals are an ERROR frame with `message: UNAUTHORIZED` and no body, then the connection closes. The captured output contains no token or key. `./mvnw -B verify`: 33 unit and 22 integration tests pass.
- 2026-09-26: T4 done. `DestinationPolicy` follows API 8.2: every client may subscribe to `/user/queue/time-sync` and send to `/app/time-sync`, and nothing unlisted is allowed. The interceptor refuses other SUBSCRIBE and SEND frames with ERROR `FORBIDDEN` (a reading: API 8.2 names no code; see DI-37). `DestinationPolicyTest` has 28 rows. In `StompConnectionIT`, a player subscribing to another game's screen and a projector sending an answer are both refused. `./mvnw -B verify`: 61 unit and 23 integration tests pass.
- 2026-09-26: T5 done. `engine.command` now has a sealed `Command` with `Reconnect`, `Disconnect` and `ClientSubscribed`, plus the `ClientRole` enum (LLD 5.4.3; the rest of the family comes with its stories). `StompEventListener` submits the commands to the `GameCommands` port. It sends the full state from the `CurrentState` port only after the broker has registered the subscription, using an `ExecutorChannelInterceptor`, so nothing can arrive before the client listens (LD-08). No-op defaults live in `GatewayDefaults`, marked TODO(EN-05), TODO(US-02) and TODO(US-04). The IT's admin handshake uses a test-only HTTP Basic chain. `StompConnectionIT`: player, projector and admin each get their state after subscribing and not before (AC-EN04-01). `./mvnw -B verify`: 61 unit and 26 integration tests pass.
- 2026-09-26: T6 done. `reconnect.ts` has `reconnectDelayMs` (0.5 s, 1 s, 2 s, then 2 s) and `createReconnectSchedule`, which takes an injected timer and never reads the clock. The status becomes `reconnecting` only after the first failed attempt, and a quick first retry keeps the banner hidden. `reconnect.test.ts` AC-EN04-04 with fake timers: during a 20-second drop, attempts follow the schedule and the connection resumes within 5 s of the network returning. Frontend format, lint and typecheck are clean; 58 tests pass.
