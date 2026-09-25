# Digest: 08 — Low-Level Design (LLD)

Source: `docs/08-lld.md`, version 1.3 (approved; 1.0 to 1.2 on 2026-09-23, 1.3 on 2026-09-24). Depends on Charter v1.5 (DEC-01 to DEC-138), SRS v1.1, HLD v1.1. Feeds 09, 10, 11, 13, 15.

## Completeness

- Line count: 755 (`wc -l`), read lines 1 to 755 in full.
- Last heading: `## 10. Approval`.
- Last line: `| Owner and approver | [Owner name] | ☑ Approved | 2026-09-23 |`.

## Purpose

The LLD specifies the internal design of each HLD component: packages, classes, data types, algorithms, threading, configuration and error handling for the backend, and structure, state and behavior for the frontend (section 1). It is the blueprint the owner codes from; physical tables are in document 10 and wire formats in document 11 (section 2).

## Every ID the document defines

### Design decisions (section 7; recorded as DEC-139 to DEC-146, Charter v1.6)

Mapping is sequential by the stated range (LD-01 = DEC-139 ... LD-08 = DEC-146); the document gives only the range.

- LD-01: tasks issued only to connected players; a disconnected player's open task keeps its timer, but no new task is issued until reconnect; matches AC-US05-03 (sections 5.4.4, 5.4.10, 7).
- LD-02: projector connections may send only time-sync requests; every other send rejected (SRS 3.5, FR-052) (sections 5.6, 6.4, 7).
- LD-03: players tied at a place in the final ranking are revealed together in one countdown step (BR-09) (sections 5.4.9, 7).
- LD-04: after a restart during Results, phones show "This game has finished." and admin labels the game "Results (live details lost after restart)" (DEC-124) (sections 5.8, 7).
- LD-05: every game state change written to the DB asynchronously, in order; start-up cleanup and the seed loader's lock check rely on it (sections 5.8, 5.10, 7).
- LD-06: REST errors use RFC 9457 Problem Details with a stable `code`; frontend maps codes to SRS messages (sections 4, 5.12, 7).
- LD-07: root Java package `app.deliveryhero`, one sub-package per HLD component (sections 4, 5.1, 7).
- LD-08: a client's full initial state is sent once its subscription is confirmed, not at connection time (sections 5.4.10, 7).

### Decisions cited from the revision history

- DEC-157: renamed `ProblemWordsContent.code` to `monospace` (revision 1.1).
- DEC-179: frontend `src/copy.ts` added (revision 1.2).
- Revision 1.3: seed command process never runs `StartupCleanup` or `HousekeepingJob` (sections 5.8, 5.10).

### Terms (section 3)

Record (Java 21 immutable `record`); Sealed interface; Session thread (single thread per game session, DEC-125); Public view (task safe for phones, no answer fields, DEC-130); Token index (problem-word task's words numbered from 0 after splitting at whitespace); Store (Zustand container).

### Enums (section 5.2)

- `GameState { CREATED, LOBBY, PRACTICE, COUNTDOWN, LIVE, FROZEN, ENDED, REVEAL, RESULTS, CLOSED, CANCELLED }`
- `Role { MANAGER, BUSINESS_ANALYST, DEVELOPER, TESTER }`
- `Phase { PLANNING, DEVELOPMENT, TESTING, RELEASE }`
- `TaskKind { SCORED, PRACTICE, INCIDENT }`
- `TaskType { MULTIPLE_CHOICE, YES_NO, ORDER, PROBLEM_WORDS }`
- `Outcome { FULLY_CORRECT, PARTLY_CORRECT, WRONG, TIMEOUT, VOIDED }`
- Also named but not defined: `ClientRole` (5.4.3), `EndReason` with `FINISHED`, `CANCELLED` (5.4.1, 5.8), `TimerType` (5.1).

### Content and answer records (section 5.2)

- `sealed interface TaskContent permits MultipleChoiceContent, YesNoContent, OrderContent, ProblemWordsContent` (DEC-131).
- `MultipleChoiceContent(List<Option> options)`, `Option(String text, boolean correct)`.
- `YesNoContent(boolean answerYes)`.
- `OrderContent(List<Item> items)` listed in display order, `Item(String text, int correctPosition)`.
- `ProblemWordsContent(String markedText, boolean monospace)`, e.g. `"load {{fast}} and…"`.
- `sealed interface AnswerPayload permits ChoiceAnswer, YesNoAnswer, OrderAnswer, WordsAnswer`.
- `ChoiceAnswer(int optionIndex)`, `YesNoAnswer(boolean yes)`, `OrderAnswer(List<Integer> itemIndexesInChosenOrder)`, `WordsAnswer(Set<Integer> tokenIndexes)`.

### Commands (section 5.4.3)

- `sealed interface Command permits Join, Reconnect, Disconnect, ClientSubscribed, SubmitAnswer, TimerFired, BotAnswer, HostCommand`.
- `Join(String rawName, CompletableFuture<JoinResult> reply)`; `Reconnect(String tokenHash, String connectionId)`; `Disconnect(String connectionId)`; `ClientSubscribed(String connectionId, ClientRole role, UUID playerId)`; `SubmitAnswer(UUID playerId, String taskKey, AnswerPayload answer, Instant receivedAt)`; `TimerFired(TimerKey key)`; `BotAnswer(UUID botId, String taskKey, AnswerPayload answer)`.
- `sealed interface HostCommand extends Command permits OpenLobby, StartPractice, EndPractice, StartRound, VoidTask, StartReveal, NextStep, PreviousStep, RenamePlayer, RemovePlayer, Discard` with `CompletableFuture<ActionResult> reply()`.

### Timer types (section 5.4.2)

`PRACTICE_END`, `ROUND_START`, `PHASE_CHANGE` (seq = phase), `INCIDENT_START`, `FREEZE`, `ROUND_END`, `TASK_DEADLINE` (player, seq), `LOCKOUT_END` (player, seq), `INCIDENT_DEADLINE` (player), `FLUSH`.

### Reveal steps (section 5.4.9)

`MOST_MISSED` (only if a most-missed task exists), one step per distinct place from min(10, players) down to 2, `WINNER`.

### Message types (section 5.7)

`GAME_STATE`, `TASK_ISSUED`, `FEEDBACK`, `INCIDENT_START`, `TASK_RESUMED`, `PRACTICE_READY`, `RESULTS`, `REMOVED`, `GAME_ENDED` (to `/user/queue/game`); `SCREEN_STATE`, `WALL_EVENTS`, `TOP10`, `FEED_EVENT`, `INCIDENT_START`, `REVEAL_STEP` (to `/topic/games/{id}/screen`); `LIVE_STATS` (to `/topic/games/{id}/admin`). Also `TIME_SYNC` request (6.2), engine events `PlayerDone`, `TaskIssued`, `Feedback`, `PracticeReady` (5.4.4, 5.4.6).

### Error codes (section 5.12)

| Code | HTTP | Meaning | Message shown |
|---|---|---|---|
| `GAME_NOT_ACTIVE` | 404 | Join link for closed, cancelled or unknown game | "This game link isn't active. Ask the host for the current link." |
| `LOBBY_NOT_OPEN` | 409 | Game in CREATED | "The lobby isn't open yet. Hang tight!" |
| `JOINING_CLOSED` | 409 | Freeze or later | "Joining has closed for this round. Enjoy the show on the big screen!" |
| `GAME_FULL` | 409 | 100 players | "This game is full." |
| `INVALID_NAME` | 422 | Name breaks BR-16 | FR-003 naming-rules message |
| `RATE_LIMITED` | 429 | Rate limit hit | "Too many tries. Please wait a moment and try again." |
| `VALIDATION_FAILED` | 422 | Content rules broken; `errors` lists each field | Each issue's message next to its field |
| `EDIT_CONFLICT` | 409 | Optimistic lock failure | "Someone else changed this since you opened it. Reload to see their changes." |
| `TASK_IN_USE` | 409 | Deleting a task used by a run plan | "This task is used by: …", listing plans |
| `ANOTHER_GAME_OPEN` | 409 | A game is already open | "Another game is still open. Close or cancel it first." |
| `NOT_ALLOWED_NOW` | 409 | Host action invalid in current state | Panel refreshes to current state |
| `DEPLOY_LOCKED` | 423 | Seed or deploy while a game is in progress | "A game is in progress. Try again after it ends." |
| `UNAUTHENTICATED` | 401 | Missing or expired admin session | Login screen appears |

Problem Details example: `{ "type": "about:blank", "title": "Game full", "status": 409, "code": "GAME_FULL", "detail": "This game is full.", "errors": [] }`.

### Logged events (section 5.14)

`GAME_CREATED`, `STATE_CHANGED` (INFO; `state`, `test`); `PLAYER_JOINED`, `PLAYER_RECONNECTED`, `PLAYER_DISCONNECTED`, `PLAYER_REMOVED` (INFO); `ANSWER_SCORED` (INFO; `taskKey`, `outcome`, `points`, `answerMs`); `ANSWER_REJECTED` (DEBUG; `taskKey`, `reason`); `INCIDENT_STARTED`, `TASK_VOIDED`, `REVEAL_STEP` (INFO; `taskKey` or `step`); `RESULTS_PERSISTED`, `GAME_CLOSED`, `GAME_CANCELLED` (INFO; `playerCount`); `LOGIN_SUCCEEDED`, `LOGIN_FAILED`, `RATE_LIMITED` (INFO or WARN; `ip`, `limit`); `SEED_IMPORTED` (INFO; counts).

### Packages and classes (section 5.1; root `app.deliveryhero`)

| Package | HLD component | Types |
|---|---|---|
| `app.deliveryhero` | Application | `DeliveryHeroApplication` (web app, or seed loader with `seed` argument) |
| `...config` | Cross-cutting | `WebSocketConfig`, `SchedulingConfig`, `ClockConfig`, `GameProperties`, `ScoringProperties` |
| `...common` | Cross-cutting | Enums (5.2), `Names`, `TokenService`, `ApiErrorCode`, `ProblemFactory` |
| `...api.pub` | Public API | `PublicGameController`, `GameStatusResponse`, `JoinRequest`, `JoinResponse` |
| `...api.admin` | Admin API | `TaskController`, `CharacterController`, `RunPlanController`, `GameController`, `HostActionController`, `PastGameController` |
| `...api.ops` | Ops API | `DeployLockController` (health from Spring Boot Actuator) |
| `...realtime` | Realtime gateway | `StompAuthInterceptor`, `DestinationPolicy`, `RealtimeController`, `StompEventListener`, `PlayerPrincipal`, `ProjectorPrincipal`, `AdminPrincipal` |
| `...engine` | Game engine | `GameEngine`, `GameSession`, `PlayerState`, `IssuedTask`, `RoundTimeline`, `IncidentState`, `RevealState`, `NameRegistry` |
| `...engine.command` | Game engine | `Command` and its records |
| `...engine.timer` | Game engine | `TimerScheduler`, `TimerKey`, `TimerType` |
| `...scoring` | Scoring and results | `ScoringConfig`, `AnswerChecker` + 4 implementations (`MultipleChoiceChecker`, `YesNoChecker`, `OrderChecker`, `ProblemWordsChecker`), `ScoreCalculator`, `RankingService`, `MostMissedService`, `ReviewBuilder`, `HeroCardService` |
| `...broadcast` | Broadcaster | `Broadcaster`, `ScreenBatch`, `AdminBatch`, message records |
| `...content` | Content | Entities (`CharacterEntity`, `TaskEntity`, `RunPlanEntity`, `RunPlanEntryEntity`), repositories, `TaskService`, `CharacterService`, `RunPlanService`, `ContentValidator`, `ReadinessChecker`, `TaskContent` types, `PublicTaskView` |
| `...lifecycle` | Game lifecycle | `GameEntity`, `TopTenEntryEntity`, repositories, `GameLifecycleService`, `SnapshotFactory`, `GameStateRecorder`, `HousekeepingJob`, `StartupCleanup`, `DeployLockService` |
| `...simulation` | Simulation | `BotDriver`, `BotProfile` |
| `...seed` | Seed loader | `SeedCommand`, `SeedFile` records, `SeedImporter` |
| `...security` | Security | `SecurityConfig`, `LoginHandlers`, `RateLimiter`, `RateLimitFilter` |

Other named types: `ValidationReport(List<Issue> errors, List<Issue> warnings)`, `Issue(String path, String code, String message)` (5.3); `GameSnapshot`, `JoinResult`, `ActionResult` (with `ActionResult.unchanged(currentState)`), `AnswerRecord`, `Evaluation(BigDecimal share)`, `Standing(playerId, name, total, fullyCorrect, averageAnswerMs, lastScoreChangeAt)`, `BotProfile(accuracy, pace)` (5.4 to 5.11); `ThreadPoolTaskScheduler` (5.6).

### Class members (section 5.4.1)

- `GameEngine`: `Map<UUID,GameSession> sessions`; `create(GameEntity, GameSnapshot)`, `find(UUID)`, `findByCode(String)`, `submit(UUID, Command)`, `discard(UUID, EndReason)`, `isAnyGameInProgress()`.
- `GameSession`: `id`, `code`, `state`, `snapshot`, `timeline`, `Map<UUID,PlayerState> players`, `Map<String,UUID> tokenIndex`, `Set<String> voidedTasks`, `incident`, `reveal`, `ExecutorService thread`; `enqueue(Command)`, private `handle(Command)`.
- `PlayerState`: `id`, `name`, `tokenHash`, `connected`, `removed`, `simulated`, `long total`, `int streak`, `int bestStreak`, `int cursor`, `IssuedTask current`, `Instant lockoutUntil`, `List<AnswerRecord> answers`, `Instant lastScoreChangeAt`. Pseudocode also uses `awaitingIssue`, `done`, `seq`, `lockoutSeq` and a practice cursor.
- `IssuedTask`: `taskKey`, `long seq`, `issuedAt`, `deadline`, `long pausedMs`.
- `TimerScheduler`: `schedule(UUID gameId, TimerKey key, Instant at)`, `cancel(UUID gameId, TimerKey key)`, `cancelAll(UUID gameId)`.

### Frontend components and files (sections 6.1 to 6.5)

- `frontend/app/`: `layout.tsx` (self-hosted fonts, theme), `page.tsx` ("Scan the QR code on the big screen"), `join/page.tsx` (reads `?code=`), `screen/page.tsx` (reads `?key=`), `admin/` (login, tasks, tasks/edit `?id=`, characters, run-plans, run-plans/edit `?id=`, games, past-games).
- `frontend/src/ui/`: `ArcadeButton`, `SpeechBubble`, `TimerBar`, `CodeBlock`, `Badge`, `Modal`, `QrCode`, `PixelIcon`.
- `src/realtime/`: `stompClient.ts`, `useStomp.ts`, reconnect schedule. `src/time/`: `timeSync.ts`, `useCountdown.ts`. `src/api/`: `http.ts`, `endpoints.ts`. `src/browser/isSupportedChrome.ts` (BR-19). `src/player/`: `store.ts`, `screens/`, `tasks/`. `src/screen/`: `store.ts`, `views/` (Lobby, Practice, Live, Wall, Top10, Feed, PhaseBar, Clock, Reveal). `src/admin/`: `store.ts`, `components/`. `src/types/`: `messages.ts`, `dto.ts` (mirroring document 11). `src/copy.ts` (all user-facing strings from document 12's copy deck, DEC-179). `scripts/csp-hashes.mjs`.
- Task components: `MultipleChoiceTask`, `YesNoTask`, `OrderTask`, `ProblemWordsTask`, `CodeBlock`.
- `interface PlayerStore` (6.3); player screen states: Checking, Notice, JoinForm, Restoring, Lobby, Practice, Countdown, Task, Lockout, Incident, Done, TimesUp, Results, Finished, plus `'removed'` and `'ended'`.

## What implementation must do

### Conventions (section 4)

- Times are `java.time.Instant` UTC from one injected `java.time.Clock`. Durations are milliseconds in messages, `java.time.Duration` in code.
- Points use `BigDecimal`, rounded once at the end (DEC-91).
- Root package `app.deliveryhero` (LD-07).
- Errors: RFC 9457 Problem Details with stable `code` (LD-06).

### Architecture rules (section 5.1)

Checked by an architecture test (document 13): `engine` doesn't depend on `api`, `content` repositories or `lifecycle` repositories; `scoring` depends on nothing but `common`; nothing depends on `api`.

### Content and validation (section 5.3)

- Entities `CharacterEntity` (key: role), `TaskEntity`, `RunPlanEntity`, `RunPlanEntryEntity`, each with `@Version` for optimistic locking (FR-073).
- JSON columns (task `code` and `content`, character lines) mapped with `@JdbcTypeCode(SqlTypes.JSON)` onto `String` fields; services convert to/from records with the app's JSON mapper, choosing the record class from the task's `type` column.
- `ContentValidator` holds every rule in SRS 7.3; returns `ValidationReport`; `Issue.path` names the field, e.g. `options[1].text`. Callers: `TaskService`/`CharacterService`/`RunPlanService` (one item; errors refuse save with HTTP 422 `VALIDATION_FAILED`); `ReadinessChecker` (run plan per BR-13; errors listed, game can't be created); `GameLifecycleService.create` (FR-077 minimal rules; creation refused); `SeedImporter` (whole seed file; nothing imported).
- Problem-word tokens: at snapshot build, split `markedText` at whitespace; remove `{{` and `}}`; keep marked token indexes privately; public view carries only token strings.
- `PublicTaskView` (DEC-130) is the only task shape leaving the engine before Results: `key`, `type`, `role`, `characterName`, `prompt`, `code`, `timeLimitMs` always; type-specific: MC option texts; yes/no none; order item texts in display order; problem words tokens and `monospace` flag; never any answer data.
- A unit test serializes every public message type built from every seed task and fails if any correct-answer data appears (NFR-12).

### Threads and timers (section 5.4.2)

- Each `GameSession` owns a single-thread executor (DEC-125); `enqueue` only adds; `handle` runs on that thread.
- `TimerScheduler` wraps one shared `ScheduledExecutorService` with two threads; firing enqueues `TimerFired(key)`, never touches the session (DEC-126).
- Per-player timers carry `seq`; on `TimerFired` compare with player's current seq, ignore if different.

| Timer | Fires at | Effect |
|---|---|---|
| `PRACTICE_END` | Practice start + 30 s | Practice ends, state back to LOBBY |
| `ROUND_START` | Round start (now + 5 s) | State LIVE; first task to every connected player |
| `PHASE_CHANGE` | Each boundary in SRS 3.2 | Screen phase bar updates |
| `INCIDENT_START` | Incident moment | Section 5.4.5 |
| `FREEZE` | L − 30 s | State FROZEN; top 10 frozen; joining closed |
| `ROUND_END` | L | State ENDED; open tasks become timeouts |
| `TASK_DEADLINE` | Deadline + 500 ms | Timeout for that task |
| `LOCKOUT_END` | Lockout end | Next task, or resume after incident |
| `INCIDENT_DEADLINE` | Incident deadline + 500 ms | Incident timeout for that player |
| `FLUSH` | Every 500 ms from LOBBY to RESULTS | Build and send screen and admin batches |

### Command state rules (section 5.4.3)

- `Join`: LOBBY, PRACTICE, COUNTDOWN, LIVE. `Reconnect`/`Disconnect`/`ClientSubscribed`: any until CLOSED. `SubmitAnswer`: PRACTICE, LIVE, FROZEN. `OpenLobby`: CREATED → LOBBY, start `FLUSH`. `StartPractice`/`EndPractice`: LOBBY, PRACTICE. `StartRound`: LOBBY with at least one player. `VoidTask`: LIVE, FROZEN, ENDED. `StartReveal`/`NextStep`/`PreviousStep`: ENDED, REVEAL. `RenamePlayer`/`RemovePlayer`: LOBBY (BR-16 renaming; removal ends the token). `Discard`: any (cancel all timers, send `GAME_ENDED`, drop session). `TimerFired`/`BotAnswer`: per timer or bot.
- A host command not applicable to the current state completes with `ActionResult.unchanged(currentState)` so the admin panel refreshes (FR-081).

### Task flow algorithm (section 5.4.4)

- `issueNext(player)`: if not connected set `awaitingIssue = true` and return (LD-01); skip voided keys in `snapshot.order` from `cursor`; if none left set `done`, emit `PlayerDone`; else take `snapshot.order[cursor++]`, `current = IssuedTask(key, ++seq, now, now + limit, pausedMs = 0)`, schedule `TASK_DEADLINE(player, seq)` at deadline + grace, emit `TaskIssued(player, publicView(task), deadline)`.
- `onSubmitAnswer`: reject unless state in {LIVE, FROZEN}, player present and not removed, `current != null`, `cmd.taskKey == current.taskKey`, `now >= lockoutUntil`, `cmd.receivedAt <= current.deadline + grace`. Then `t = clamp(receivedAt − issuedAt − pausedMs, 0, limit)`; `scoreCalculator.scored(task, answer, t, streak)`; apply total, streak, bestStreak, answers, `lastScoreChangeAt = now`; cancel `TASK_DEADLINE`; `current = null`; emit `Feedback(player, result, reactionLine(role, outcome))`; if WRONG set `lockoutUntil = now + lockout` and schedule `LOCKOUT_END(player, ++lockoutSeq)`; else `issueNext`.
- `onTaskDeadline(player, seq)`: stale if `current == null` or seq differs; else record TIMEOUT (0 points, streak = 0), `current = null`, emit timeout feedback, `issueNext`.
- Rejections change no score; logged at DEBUG with reason and counted in admin statistics.

### Incident (section 5.4.5)

- On `INCIDENT_START`: for each connected, non-removed player save `pausedTaskMs = deadline − now` (if task open) and `pausedLockoutMs = lockoutUntil − now` (if locked out), cancel both timers; send `INCIDENT_START` with deadline now + 20 s; schedule `INCIDENT_DEADLINE` per player at deadline + 0.5 s. Paused time added to `IssuedTask.pausedMs` so answer time excludes it (FR-037).
- Incident answer scored with BR-08, streak unchanged (DEC-86).
- Disconnected players marked pending; reconnect before the incident deadline gets it with remaining time; otherwise skipped (FR-046).
- Resume after answer or incident timeout: wrong incident answer → 3-second lockout first; then any paused lockout rescheduled for its remaining time, followed by the next task; otherwise the paused task gets a new deadline of now + remaining time and a `TASK_RESUMED` message. A player who was done returns to the done screen.

### Practice (section 5.4.6)

`StartPractice` → PRACTICE, schedule `PRACTICE_END` 30 s later, give each connected player a separate practice cursor over the snapshot's practice list. Same checkers; feedback including 3-second lockout on wrong; nothing added to totals, streaks, answers or statistics (FR-015). Finisher gets `PracticeReady` ("Ready!"). `PRACTICE_END` or `EndPractice` cancels practice timers → LOBBY. Joiners during practice get no practice tasks (DEC-115).

### Round timeline (section 5.4.7)

```java
public record RoundTimeline(Instant start, int lengthSec, int planningEnd, int developmentEnd,
                            int testingEnd, Integer incidentAtSec, int freezeAtSec) {
    static RoundTimeline of(Instant start, int L, boolean hasIncident, RandomGenerator rnd) {
        int p = (int) Math.floor(0.2 * L), d = (int) Math.floor(0.6 * L), t = (int) Math.floor(0.8 * L);
        int w = t - d;
        Integer incident = hasIncident
            ? d + rnd.nextInt((int) Math.ceil(0.1 * w), (int) Math.floor(0.9 * w) + 1)
            : null;
        return new RoundTimeline(start, L, p, d, t, incident, L - 30);
    }
}
```

`StartRound` → COUNTDOWN, `start = now + 5 s`; schedules `ROUND_START`, three `PHASE_CHANGE`, `INCIDENT_START` (if present), `FREEZE`, `ROUND_END`. Incident moment never in any message (FR-043). Unit test checks the three rows of SRS 3.2's table (BR-18).

### Voiding (section 5.4.8, BR-14)

Add key to `voidedTasks`; for every player find the `AnswerRecord`, subtract its points, mark voided; a player on the task: cancel deadline, `VOIDED` record with 0 points, `issueNext`. Players not yet there skip it. Streaks and lockouts unchanged. Top 10 marked dirty, resent within 500 ms (FR-083).

### Reveal and results (section 5.4.9)

`StartReveal` builds the final `List<Standing>` and `RevealState` steps (`MOST_MISSED` if found; one step per distinct place from min(10, players) to 2; `WINNER`); ties share one step (LD-03). `NextStep` reaching `WINNER` → RESULTS, each player gets `RESULTS` (rank, total, review entries, hero card), summary and top 10 to `GameLifecycleService.persistResults` off the session thread (DEC-124). `PreviousStep` moves back unless current step is `WINNER` (FR-063).

### Join, reconnect, disconnect (section 5.4.10)

- Flow: `POST /api/games/K7PQ2M/players` with name → `Join` with reply future → check state and capacity, normalize, de-duplicate, create player, new token → `201` with final name and token → CONNECT with `player-token` header → `Reconnect(token hash, connection ID)` → SUBSCRIBE `/user/queue/game` → `ClientSubscribed` → full `GAME_STATE`.
- Join (FR-003 to FR-006, FR-012): `Names.normalize` (NFC, trim, collapse spaces; DEC-120) → validation → `NameRegistry.unique(base)` adds " 2", " 3", shortening the base to fit 20 characters (BR-16). `TokenService`: 16 random bytes, URL-safe Base64 without padding (22 characters); keep only SHA-256 hash in `tokenIndex` (DEC-109). Controller waits up to 2 seconds for the reply.
- Reconnect: bind, mark connected; if `awaitingIssue` and state LIVE or FROZEN run `issueNext`.
- ClientSubscribed: full state sent only after subscription confirmed (LD-08); projector/admin get screen snapshot or live statistics.
- Disconnect: mark offline, add offline wall event; timers keep running (DEC-89); no new task until reconnect (LD-01).

### Scoring (section 5.5)

`scoring.yml` (DEC-28), prefix `dh.scoring`: `base: 100`, `max-bonus: 50`, `wrong-penalty: 40`, `yes-no-wrong-penalty: 100`, `incident-base: 200`, `incident-max-bonus: 100`, `incident-wrong-penalty: 80`, `streak-multiplier: 1.5`, `streak-threshold: 3`, `partial-threshold: 0.5`, `lockout: 3s`, `grace: 500ms`.

Checkers return `Evaluation(BigDecimal share)`: MC 1 if chosen option correct else 0; yes/no 1 if match else 0; Order = items whose chosen position equals `correctPosition` ÷ number of items (BR-05); ProblemWords = min(1, max(0, c − w) ÷ k) (BR-06).

`ScoreCalculator` with `MathContext.DECIMAL64` (BR-02, 03, 04, 07, 08):

```text
outcome = share == 1 ? FULLY_CORRECT : share >= 0.5 ? PARTLY_CORRECT : WRONG
if WRONG: points = -(incident ? 80 : type == YES_NO ? 100 : 40); lockout = true
else:
    bonus  = maxBonus × (limitMs − t) ÷ limitMs
    m      = (!incident && FULLY_CORRECT && streakBefore >= 3) ? 1.5 : 1
    points = (base + bonus) × share × m, rounded HALF_UP to a whole number
newStreak = incident ? streakBefore : (FULLY_CORRECT ? streakBefore + 1 : 0)
```

Parameterized test covers every worked example in PRD 8.3 and every scoring criterion in document 05.

`RankingService` (BR-09): total desc, fullyCorrect desc, averageAnswerMs asc nulls last, lastScoreChangeAt asc nulls last. Competition ranking (ties on all four keys share a rank; next rank skips). Removed players and voided answers excluded. `MostMissedService` (BR-10), `ReviewBuilder` (BR-11), `HeroCardService` (BR-12) implement rules exactly, each unit-tested; hero card thresholds 0.5 of time limit and 0.75 accuracy from `scoring.yml`.

### Realtime gateway (section 5.6)

- `WebSocketConfig`: STOMP endpoint `/ws`, allowed origins limited to the site's own origin, no SockJS (DEC-127); simple broker for `/topic` and `/queue`, heartbeats 10,000 ms both directions via `ThreadPoolTaskScheduler`; application prefix `/app`, user prefix `/user`; inbound message size limit 8 KB.
- `StompAuthInterceptor` (client inbound channel, DEC-133): CONNECT with `player-token` → hash, look up in current game's `tokenIndex` → `PlayerPrincipal(gameId, playerId)`, name `p:<playerId>`; CONNECT with `projector-key` → constant-time compare with the current game's key → `ProjectorPrincipal(gameId)`; CONNECT on authenticated admin session → `AdminPrincipal`; otherwise ERROR frame. SUBSCRIBE → `DestinationPolicy.maySubscribe(principal, destination)` per HLD 11. SEND → `DestinationPolicy.maySend`; projectors only `/app/time-sync` (LD-02).
- `RealtimeController`: `@MessageMapping("/games/{gameId}/answer")` stamps `receivedAt` from the clock first, applies per-player answer rate limit, enqueues `SubmitAnswer`. `@MessageMapping("/time-sync")` with `@SendToUser("/queue/time-sync")` replies `{clientSentAt, serverTime}` without touching the engine.
- `StompEventListener`: `SessionConnectedEvent` → `Reconnect`, `SessionSubscribeEvent` → `ClientSubscribed`, `SessionDisconnectEvent` → `Disconnect`. Broker closes a connection without heartbeats within 20 seconds (DEC-122).

### Broadcaster (section 5.7)

Hands messages to Spring's outbound channel (own threads). Batching: between flushes accumulate wall events (player ID and state), feed events and `top10Dirty`; on `FLUSH` build one `ScreenBatch` (omit top 10 while frozen) and one `AdminBatch`, send only if non-empty. Wall events carry initials and a state, never points (FR-056). Destinations/timing as listed under message types: `GAME_STATE` on subscribe and every state change; player events immediately; `RESULTS` at winner step; `SCREEN_STATE` on subscribe and state change; `WALL_EVENTS`, `TOP10`, `FEED_EVENT`, `LIVE_STATS` in 500 ms batch (DEC-128); screen `INCIDENT_START`, `REVEAL_STEP` immediately.

### Game lifecycle (section 5.8)

- `create(runPlanId, test, botCount)`: one transaction: check no game outside CLOSED and CANCELLED (DEC-101); validate plan (FR-077); `SnapshotFactory` copies plan, tasks, characters into `GameSnapshot`; generate unique code (BR-17) and projector key; save `GameEntity` in CREATED; after commit `GameEngine.create(...)`.
- `GameStateRecorder`: writes each state change to the game row on its own single-thread executor, in order, never blocking a session (LD-05).
- `persistResults(gameId, summary, top10)`: one transaction: state RESULTS, `results_at`, player count, top-10 rows.
- `close(gameId)`: requires RESULTS; one transaction: CLOSED, `closed_at`, projector key cleared; after commit `Discard(FINISHED)`; test game row deleted instead (FR-085).
- `cancel(gameId)`: allowed before RESULTS; one transaction: CANCELLED, key cleared; after commit `Discard(CANCELLED)`.
- `HousekeepingJob` (every minute, web app only): close real games in RESULTS for 24 hours (FR-088); delete test games in RESULTS for 2 hours (FR-085).
- `StartupCleanup` (on application ready, web app only): LOBBY through REVEAL → CANCELLED, clear keys; delete test games not in CREATED (FR-089).
- `DeployLockService`: locked if any session in LOBBY through REVEAL (FR-090).
- `StartupCleanup` and `HousekeepingJob` use `@ConditionalOnWebApplication`; seed command must refuse, never cancel (5.10).
- After restart in RESULTS: phones get "This game has finished."; admin shows "Results (live details lost after restart)" with Close available (LD-04).

### Security (section 5.9, DEC-132)

- Filter chain: `POST /api/admin/login` open, rate-limited by `RateLimitFilter`; `/api/admin/**` authenticated admin, CSRF on POST, PUT, PATCH, DELETE; `/api/games/**` open, join rate-limited per IP; `/api/ops/**` open inside Compose network only, Nginx returns 404 (DEC-137); `/actuator/health` open (Nginx proxies as `/health`); `/ws` open at HTTP level, authenticated at STOMP CONNECT; anything else denied.
- Login: single user `admin`, hash from `DH_ADMIN_PASSWORD_HASH` (bcrypt cost 12 or more; DEC-98). Form login at `/api/admin/login` returning 204 or 401 (no redirects). Logout `POST /api/admin/logout`.
- Session: `server.servlet.session.timeout=12h`; cookie HttpOnly, Secure, SameSite=Strict (DEC-97).
- CSRF: cookie-based token for SPAs; frontend reads `XSRF-TOKEN` cookie, sends `X-XSRF-TOKEN` header.
- Client IP: `server.forward-headers-strategy=native` with internal proxies set; trust `X-Forwarded-For` only from the Nginx container.
- `RateLimiter`: in-memory fixed windows: `login:<ip>` 5 failures per 15 minutes then 15-minute block; `join:<ip>` 120 per minute; `answer:<playerId>` 5 per second. Exceeding → 429 `RATE_LIMITED` or STOMP message dropped.

### Seed loader (section 5.10)

1. `docker compose run --rm backend seed /seed/delivery-hero-seed.json` starts without web server (DEC-136); no cleanup/housekeeping beans.
2. `SeedCommand` refuses if any game row is in LOBBY through REVEAL (LD-05).
3. `SeedImporter` parses into `SeedFile` records, runs `ContentValidator` on everything; on errors prints each with its key, exits status 1.
4. One transaction: upsert characters by role, tasks by key, run plans by key (replacing entries); print counts; exit 0.

### Simulation (section 5.11)

`BotDriver` creates bots ("Bot 01" onward) when a test game's lobby opens, each `BotProfile(accuracy, pace)` per BR-15. On task issue, schedule `BotAnswer` after `pace × limit × U(0.8, 1.2)`, capped at 0.95 × limit; correct with probability `accuracy`; for partial-credit tasks pick an order/word selection with share in the BR-15 range. Bots always connected, receive no messages, skipped by the Broadcaster.

### Configuration (section 5.13)

`dh.game.max-players` 100 (FR-006); `dh.game.countdown` 5s (FR-019); `dh.game.practice` 30s (FR-014); `dh.game.freeze` 30s (FR-050); `dh.game.auto-close` 24h (FR-088); `dh.game.test-retention` 2h (FR-085); `dh.broadcast.batch-interval` 500ms (DEC-128); `dh.rate.login-failures`, `dh.rate.join-per-minute`, `dh.rate.answers-per-second` 5, 120, 5 (DEC-108); `dh.admin.password-hash` from `DH_ADMIN_PASSWORD_HASH` (DEC-98); `spring.datasource.*` from env vars; `server.servlet.session.timeout` 12h (DEC-97); `logging.structured.format.console` `ecs` (NFR-11); `spring.config.import` `classpath:scoring.yml` (DEC-28).

### Logging (section 5.14)

Structured JSON with `gameId`, `playerId` where relevant, `event`; never names, answers or password (DEC-104).

### Frontend (section 6)

- Routes use query parameters, not dynamic segments (static export) (6.1).
- `stompClient.ts` wraps `@stomp/stompjs` (DEC-134): `player-token` or `projector-key` in CONNECT headers; 10-second heartbeats; own reconnect schedule 0.5 s, 1 s, 2 s, then every 2 s; exposes status for "Reconnecting…"; re-subscribes after every reconnect (6.2).
- `timeSync.ts`: three `TIME_SYNC` requests on connect and every 60 s; keep the shortest round trip; `offset = serverTime − (clientSentAt + roundTrip ÷ 2)`. `useCountdown(deadlineServerMs)` renders `deadline − (Date.now() + offset)`, at least 10 updates per second (6.2).
- `http.ts`: JSON, `X-XSRF-TOKEN` from cookie on state-changing calls, Problem Details → typed error with `code` and field `errors` (6.2).
- `isSupportedChrome.ts`: BR-19 on `navigator.userAgent` (6.2).
- Player state machine (6.3): Checking → Notice (not Chrome) → JoinForm (continue anyway); Checking → JoinForm (Chrome, no saved token); Checking → Restoring (saved token) → Lobby / Task / Finished; JoinForm → Lobby (joined) or Task (joined late); Lobby ↔ Practice; Lobby → Countdown → Task; Task → Lockout → Task; Task → Incident → Task; Task → Done; Task/Done → TimesUp → Results (winner shown) → Finished (event closed).
- `PlayerStore`: `code`, `token?` (in localStorage as `dh.token.<code>`), `name?`, `screen`, `total`, `streak`, `task?: { view, deadline }` (server epoch ms), `lockoutUntil?`, `incident?`, `results?: ResultsMessage`, `connection: 'connecting' | 'online' | 'reconnecting'`, `serverOffsetMs`.
- Task components disable input as soon as an answer is sent and wait for `FEEDBACK`. `MultipleChoiceTask`: large buttons in stored order, first tap submits `{optionIndex}`. `YesNoTask`: pointer horizontal movement, release beyond 25% of width submits yes (right) or no (left); Yes and No buttons too (DEC-78). `OrderTask`: tap numbers next item; Undo removes last; Submit sends `itemIndexesInChosenOrder` once all numbered. `ProblemWordsTask`: tokens are toggle buttons; Submit sends `tokenIndexes`. `CodeBlock`: monospace, whitespace preserved, own horizontal scroll (FR-033).
- Projector (6.4): store holds latest `SCREEN_STATE`, applies `WALL_EVENTS` to a map of squares, replaces top 10 on each `TOP10` unless frozen; CSS grid sized from player count, 100 squares fit 1920×1080 without scrolling (about 10 columns); each square: initials, first name, state icon (FR-056); `REVEAL_STEP` swaps the whole view (most-missed, place card, winner); only outgoing messages are time-sync (LD-02).
- Admin (6.5): pages as listed; `VALIDATION_FAILED` field errors placed via each issue's `path`; live control: large buttons for valid actions, confirmation dialog for Cancel and Close, `LIVE_STATS`, keyboard handler when focus not in a text field: Next = Right arrow, Down arrow, Page Down, Space, Enter; Back = Left arrow, Up arrow, Page Up (DEC-112).
- Build (6.6): `next.config` `output: 'export'`, `trailingSlash: true`, unoptimized images, targets supported Chrome (DEC-111). `scripts/csp-hashes.mjs` after `next build` hashes inline `<script>` without `src` (SHA-256), writes `nginx/csp.conf` with: `script-src 'self'` + hashes, `style-src 'self' 'unsafe-inline'`, `connect-src 'self'`, `img-src 'self' data:`, `font-src 'self'`, `object-src 'none'`, `base-uri 'self'`, `frame-ancestors 'none'`; Nginx includes it (DEC-135). Playwright test loads each page with the header and fails on any CSP console violation.
- Accessibility (6.7): real `<button>`/`<input>` with accessible names; icons have text labels; feedback and task timer use polite live regions, timer announces every 5 seconds; theme tokens meet NFR-25 in dark mode; color always paired with icon (NFR-26); respect `prefers-reduced-motion`, nothing pulses faster than once per second (NFR-29, NFR-34); relative units for 200% text and 320-pixel widths (NFR-30).

### Traceability (section 8)

FR-001 to FR-013: `PublicGameController`, `GameSession` (join), `Names`, `NameRegistry`, `TokenService`, `isSupportedChrome`. FR-014 to FR-028: `GameSession` (practice, task flow), `RoundTimeline`, `TimerScheduler`, task components. FR-029 to FR-042: task components, checkers, `ScoreCalculator`, `Broadcaster`. FR-043 to FR-051: `GameSession` (incident, freeze), `IncidentState`, projector views. FR-052 to FR-058: `StompAuthInterceptor`, `DestinationPolicy`, `ScreenBatch`, projector store and views. FR-059 to FR-066: `RevealState`, `RankingService`, `MostMissedService`, `ReviewBuilder`, `HeroCardService`. FR-067 to FR-078: `SecurityConfig`, `RateLimiter`, content services, `ContentValidator`, `ReadinessChecker`, `SeedImporter`. FR-079 to FR-093: `GameLifecycleService`, `GameStateRecorder`, `HousekeepingJob`, `StartupCleanup`, `DeployLockService`, `BotDriver`. BR-01 to BR-09: checkers, `ScoreCalculator`, `RankingService`. BR-10 to BR-12: `MostMissedService`, `ReviewBuilder`, `HeroCardService`. BR-13: `ReadinessChecker`. BR-14: `GameSession` (void). BR-15: `BotDriver`, `BotProfile`. BR-16, BR-17, BR-19: `Names`, `NameRegistry`, `TokenService`, `isSupportedChrome`. BR-18: `RoundTimeline`.

## Ordering and dependencies

- `common` (enums, `Names`, `TokenService`, `ApiErrorCode`, `ProblemFactory`) and `config` (`ClockConfig`, properties) first; `scoring` depends only on `common` so it can be built and tested early (5.1).
- Content entities, Flyway tables (document 10), `ContentValidator` before services, `ReadinessChecker`, `SeedImporter`, `SnapshotFactory` (5.3, 5.8, 5.10).
- `GameStateRecorder` (LD-05) must exist before `StartupCleanup`, `DeployLockService` DB checks and the seed lock check are reliable (5.8, 5.10).
- Lifecycle `create` commits before `GameEngine.create`; close/cancel commit before `Discard` (5.8).
- `TimerScheduler` and command queue before task flow, practice, incident, reveal (5.4).
- Realtime: `StompAuthInterceptor` + `DestinationPolicy` before `RealtimeController`; subscription must be confirmed before initial state is sent (LD-08).
- Security filter chain, CSRF, rate limiter before admin API is exposed (5.9).
- Frontend: `copy.ts`, types (from document 11), `http.ts`, `stompClient.ts`, `timeSync.ts` before area screens (6.1, 6.2). `scripts/csp-hashes.mjs` runs after `next build`, output consumed by Nginx (6.6).
- Package root name must be fixed before the first commit (section 4).

## Dates and milestones

- 0.1, 1.0, 1.1, 1.2: 2026-09-23; 1.3: 2026-09-24; approval row dated 2026-09-23 (document control, section 10).
- No sprint dates in this document.

## Owner-only actions

- Decide the root package: keep `app.deliveryhero` or replace with the organization's reverse domain before the first commit (section 4).
- Provide `DH_ADMIN_PASSWORD_HASH` (bcrypt, cost 12 or more) and database credentials via environment variables (5.9, 5.13).
- Run the seed command with the approved seed file `/seed/delivery-hero-seed.json` (5.10); task content itself is owner-supplied.
- Configure Nginx trusted-proxy setup (`server.forward-headers-strategy=native` with internal proxies) consistent with the Compose network (5.9).

## Easy to get wrong

- Stamp `receivedAt` from the injected `Clock` before rate-limiting and enqueueing (5.6); deadline check uses `receivedAt`, not processing time.
- Answer time `t = clamp(receivedAt − issuedAt − pausedMs, 0, limit)`; incident pause must accumulate into `pausedMs` (5.4.4, 5.4.5).
- Two sequence counters: task `seq` and `lockoutSeq`; stale-timer checks must compare against the right one (5.4.4).
- Timeout resets streak to 0 (5.4.4); incident never changes streak (DEC-86); voiding leaves streaks and lockouts (5.4.8).
- Streak multiplier applies only when not incident, FULLY_CORRECT and `streakBefore >= 3` (uses the streak before this answer) (5.5).
- Wrong penalties: −40 standard, −100 yes/no, −80 incident; partial = share ≥ 0.5 and < 1; share < 0.5 is WRONG with lockout (5.5). Round once, HALF_UP, DECIMAL64.
- Order share compares chosen position with `correctPosition`; items given in display order (5.2, 5.5).
- Problem words: split at whitespace, strip `{{`/`}}`; public view carries tokens only; marked indexes private (5.3).
- Disconnected players never get a new task (LD-01); on reconnect in LIVE/FROZEN with `awaitingIssue`, `issueNext` runs (5.4.10).
- Initial state only on `ClientSubscribed`, not on CONNECT (LD-08).
- `FREEZE` omits top 10 from screen batches; joining closes at FREEZE (`JOINING_CLOSED` from freeze onward) (5.4.2, 5.7, 5.12).
- `FLUSH` sends only non-empty batches; wall events never carry points (5.7).
- Ties share rank (competition numbering) and share one reveal step (LD-03); `PreviousStep` can't leave `WINNER` (5.4.9).
- `persistResults` must run off the session thread (DEC-124).
- Seed process must not create `StartupCleanup` or `HousekeepingJob` (`@ConditionalOnWebApplication`), otherwise running the seed during a game would cancel it (5.8, revision 1.3).
- Projector key comparison must be constant time (5.6).
- Tokens: 16 bytes URL-safe Base64 no padding (22 chars); only SHA-256 hash stored (DEC-109).
- Name de-duplication shortens the base so the suffixed name fits 20 characters (BR-16).
- Join controller waits up to 2 seconds for the engine reply (5.4.10).
- Host command in wrong state completes with `unchanged`, not an error in the engine (5.4.3), while REST has `NOT_ALLOWED_NOW` 409 (5.12): align in document 11.
- Login handlers must return 204/401, never redirects (5.9).
- The answer rate limit drops STOMP messages silently (5.9).
- The public-DTO leak test must run over every seed task and every public message type (5.3).
- Timer announcement every 5 seconds, not every tick; countdown updates at least 10 times per second (6.2, 6.7).
- `trailingSlash: true`, query-parameter routes (6.1, 6.6).
- Architecture test enforces package dependency rules (5.1).

## Doc issues noticed

1. Section 5.4.1 vs 5.4.4: `PlayerState` class diagram lacks fields the pseudocode uses (`awaitingIssue`, `done`, `seq`, `lockoutSeq`, practice cursor, paused task/lockout ms), and section 5.4.2 says timers compare against "the player's current `seq`" without distinguishing task and lockout sequences. Suggested fix: add these fields to the diagram and state that `LOCKOUT_END` compares with `lockoutSeq`.
2. Section 5.4.4 `onSubmitAnswer` only accepts LIVE/FROZEN and requires `cmd.taskKey == player.current.taskKey`, so it would reject incident answers (the paused task stays `current`) and practice answers (allowed in PRACTICE by 5.4.3). Suggested fix: add explicit incident and practice branches (or note they are handled by `IncidentState` / practice cursor before this check).
3. Section 5.4.7 hard-codes `freezeAtSec = L − 30` while section 5.13 exposes `dh.game.freeze` (30s, FR-050) and the e2e profile uses short timings (CLAUDE.md, doc 18). Suggested fix: pass the configured freeze into `RoundTimeline.of`. Similarly the 20-second incident duration (5.4.5) and 3-second post-incident lockout are literals, not config keys.
4. Section 5.5 says hero card thresholds (0.5 of the time limit, 0.75 accuracy) come from `scoring.yml`, but the listed `scoring.yml` has no such keys; the login window/block (15 minutes) also has no config key in 5.13. Suggested fix: add the keys (names to be decided) to 5.5 and 5.13.
5. Section 5.4.5: only connected players have timers paused; a disconnected player's open task timer keeps running during the incident (DEC-89) and, if they miss the incident, is never paused, so it can time out during the incident. The doc doesn't say whether this is intended. Suggested fix: state it explicitly (or pause for pending players too).
6. Types used but never defined here: `ClientRole`, `EndReason` (`FINISHED`, `CANCELLED`), `TimerKey`, `GameSnapshot`, `JoinResult`, `ActionResult`, `AnswerRecord`, `IncidentState`, `RevealState` fields. Suggested fix: add them to section 5.2 or point to document 11.
7. Section 5.12: `DEPLOY_LOCKED` 423 covers "Seed or deploy", but the seed loader is a CLI that exits with a status (5.10 gives only statuses 0 and 1 and says it "refuses"), and the deploy lock endpoint reports status rather than failing. Suggested fix: define the seed's refusal exit code and message, and the deploy-lock response shape, in 5.10 / document 11.
8. Section 5.9 `/api/ops/**` is "open inside the Compose network only", yet the deploy script runs on the host (HLD 11, 12) and the backend port is internal. Suggested fix: document 16 must name how the script calls it.
9. Section 5.8 `DeployLockService` locks only LOBBY through REVEAL, so a deploy during RESULTS drops live review and hero cards (LD-04 accepts the aftermath but not the trigger); with every merge to `main` auto-deploying this is likely during an event. Suggested fix: decide whether RESULTS locks deploys (new DEC).
10. Section 5.11 says bots are "skipped by the Broadcaster", which is ambiguous about whether bot squares appear on the projector wall and top 10 in test games. Suggested fix: clarify "receive no player messages" vs. appearing in screen batches.
11. Section 6.1 `screen/page.tsx` reads `?key=`, putting the projector key in a URL that Nginx logs, conflicting with DEC-104. Suggested fix: use a fragment or disable query logging for `/screen`.
12. Section 6.6 writes `nginx/csp.conf`, but the repository's deploy files live under `deploy/`; the path is relative to an unspecified root. Suggested fix: give the full repository path.
