# Digest: 10 — Database Design Document (ERD)

Source: `docs/10-database-design.md`, version 1.0 (approved 23 September 2026).

## Completeness

- Line count: 540 (last line is 540; read lines 1 to 540 in two chunks).
- Last heading read: `## 18. Approval`.
- Last line read: `| Owner and approver | [Owner name] | ☑ Approved | 2026-09-23 |`.

## Purpose

Defines the physical PostgreSQL 18 database: every table, column, constraint, index and JSON format, which rules the database enforces versus the application, the Flyway migrations, and how data is written, kept and deleted. Live game data (players, tokens, answers, scores) is deliberately not stored; it lives only in backend memory (DEC-124).

## Every ID the document defines

Document control (Document control): depends on Charter v1.7 (DEC-01 to DEC-151), SRS v1.1, HLD v1.1, LLD v1.0, SAD v1.0; feeds 11, 15, 16, 18. v1.0 records DB-01 to DB-07 as DEC-152 to DEC-158 (Charter v1.8); LLD and SRS updated for the `monospace` flag. DDL was applied to real PostgreSQL 16 during drafting, full task pool imported, 16 constraint tests passed.

Terms (3): DDL, Flyway migration (`V1__…sql`), natural key, JSONB, compare-and-set update (`UPDATE … WHERE state = <expected>`), optimistic locking (`version` column).

Design decisions (16):

- DB-01 (DEC-152): DB enforces at most one open game via partial unique index; projector key must exist while open and be cleared once closed or cancelled (guards DEC-101, DEC-109) (16)
- DB-02 (DEC-153): UUID primary keys assigned by the application; unique natural keys `task_key`, `plan_key`, `role` (16)
- DB-03 (DEC-154): Migration V2 creates the four default characters (16)
- DB-04 (DEC-155): Top-10 keeps every player ranked 1st to 10th with separate display order; ties at 10th can exceed 10 rows (BR-09) (16)
- DB-05 (DEC-156): All game-row writes go through the state recorder's single thread as compare-and-set updates (16)
- DB-06 (DEC-157): Flyway owns schema; Hibernate `ddl-auto=validate`; problem-word content uses `monospace` flag (LLD `ProblemWordsContent.code` renamed `monospace`) (16)
- DB-07 (DEC-158): One least-privilege, non-superuser DB role owns the schema; DB reachable only inside the Compose network (16)

Tables (six; sections 7 and 10.1): `characters`, `tasks`, `run_plans`, `run_plan_entries`, `games`, `top_ten_entries`. No player, token or answer tables (5).

Table `characters` (7.1, 10.1):

- `role`: varchar(20) PK; CHECK in ('MANAGER', 'BUSINESS_ANALYST', 'DEVELOPER', 'TESTER')
- `display_name`: varchar(20) NOT NULL; CHECK `char_length >= 1` (1–20)
- `intro_line`: varchar(80) NOT NULL; CHECK `char_length >= 1` (1–80)
- `correct_lines`: jsonb NOT NULL; CHECK `jsonb_typeof = 'array' AND jsonb_array_length = 3`; each string 1–80 chars (application)
- `wrong_lines`: jsonb NOT NULL; same check
- `version`: integer NOT NULL DEFAULT 0; incremented every save (FR-073)
- `updated_at`: timestamptz NOT NULL DEFAULT `now()`

Table `tasks` (7.2, 10.1):

- `id`: uuid PK
- `task_key`: varchar(40) NOT NULL UNIQUE; CHECK `task_key ~ '^[a-z0-9-]{1,40}$'` (example `mgr-plan-01`)
- `role`: varchar(20) NOT NULL REFERENCES `characters (role)`
- `kind`: varchar(10) NOT NULL; CHECK in ('SCORED', 'PRACTICE', 'INCIDENT')
- `phase`: varchar(12) nullable; CHECK in ('PLANNING', 'DEVELOPMENT', 'TESTING', 'RELEASE')
- `task_type`: varchar(16) NOT NULL; CHECK in ('MULTIPLE_CHOICE', 'YES_NO', 'ORDER', 'PROBLEM_WORDS')
- `prompt`: varchar(200) NOT NULL; CHECK `char_length >= 1`
- `code`: jsonb nullable; CHECK `code IS NULL OR jsonb_typeof(code) = 'object'`
- `time_limit_seconds`: smallint nullable; CHECK BETWEEN 5 AND 60; null means type default (DEC-74)
- `content`: jsonb NOT NULL; CHECK `jsonb_typeof(content) = 'object'`
- `explanation`: varchar(300) nullable; required for scored and incident tasks by the readiness check
- `version`: integer NOT NULL DEFAULT 0
- `created_at`, `updated_at`: timestamptz NOT NULL DEFAULT `now()`
- Constraint `tasks_phase_matches_kind`: `CHECK ((kind = 'SCORED') = (phase IS NOT NULL))`
- Constraint `tasks_incident_is_multiple_choice`: `CHECK (kind <> 'INCIDENT' OR task_type = 'MULTIPLE_CHOICE')` (DEC-76)

Table `run_plans` (7.3, 10.1; DEC-37):

- `id`: uuid PK
- `plan_key`: varchar(40) NOT NULL UNIQUE; CHECK `plan_key ~ '^[a-z0-9-]{1,40}$'` (example `default-5min`)
- `name`: varchar(60) NOT NULL; CHECK `char_length >= 1` (example "Default 5-minute plan")
- `round_length_minutes`: smallint NOT NULL; CHECK BETWEEN 3 AND 10 (DEC-13)
- `incident_task_id`: uuid nullable REFERENCES `tasks (id)` (no ON DELETE); must be an incident task (application)
- `version`: integer NOT NULL DEFAULT 0
- `created_at`, `updated_at`: timestamptz NOT NULL DEFAULT `now()`

Table `run_plan_entries` (7.4, 10.1):

- `run_plan_id`: uuid NOT NULL REFERENCES `run_plans (id)` ON DELETE CASCADE
- `task_id`: uuid NOT NULL REFERENCES `tasks (id)` (blocks deleting a task in use)
- `list_name`: varchar(12) NOT NULL; CHECK in ('PRACTICE', 'PLANNING', 'DEVELOPMENT', 'TESTING', 'RELEASE')
- `sort_order`: smallint NOT NULL; CHECK `>= 0`
- PRIMARY KEY (`run_plan_id`, `task_id`): a task at most once per plan (FR-076)
- Constraint `run_plan_entries_order_unique`: UNIQUE (`run_plan_id`, `list_name`, `sort_order`) DEFERRABLE INITIALLY DEFERRED
- Index `run_plan_entries_task_idx` ON (`task_id`)

Table `games` (7.5, 10.1):

- `id`: uuid PK; used in message destinations
- `code`: varchar(6) NOT NULL; CHECK `code ~ '^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{6}$'` (BR-17)
- `projector_key`: varchar(22) nullable; present while open, cleared on close or cancel (DEC-109)
- `state`: varchar(10) NOT NULL; CHECK in ('CREATED', 'LOBBY', 'PRACTICE', 'COUNTDOWN', 'LIVE', 'FROZEN', 'ENDED', 'REVEAL', 'RESULTS', 'CLOSED', 'CANCELLED') (11 states; DEC-143)
- `is_test`: boolean NOT NULL DEFAULT false
- `run_plan_id`: uuid nullable REFERENCES `run_plans (id)` ON DELETE SET NULL
- `run_plan_name`: varchar(60) NOT NULL (copy)
- `round_length_minutes`: smallint NOT NULL; CHECK BETWEEN 3 AND 10 (copy)
- `snapshot`: jsonb NOT NULL; CHECK `jsonb_typeof(snapshot) = 'object'` (DEC-100)
- `player_count`: smallint nullable; CHECK BETWEEN 0 AND 100; set on reaching Results
- `created_at`: timestamptz NOT NULL DEFAULT `now()`
- `lobby_opened_at`, `round_started_at`, `round_ended_at`: timestamptz nullable
- `results_at`: timestamptz nullable; required in RESULTS and CLOSED; drives auto-close (FR-088)
- `closed_at`: timestamptz; required in CLOSED
- `cancelled_at`: timestamptz; required in CANCELLED
- `updated_at`: timestamptz NOT NULL DEFAULT `now()`
- Constraint `games_key_cleared_when_finished`: `CHECK (projector_key IS NULL OR state NOT IN ('CLOSED', 'CANCELLED'))`
- Constraint `games_key_present_while_open`: `CHECK (projector_key IS NOT NULL OR state IN ('CLOSED', 'CANCELLED'))`
- Constraint `games_results_time_set`: `CHECK (state NOT IN ('RESULTS', 'CLOSED') OR results_at IS NOT NULL)`
- Constraint `games_closed_time_set`: `CHECK (state <> 'CLOSED' OR closed_at IS NOT NULL)`
- Constraint `games_cancelled_time_set`: `CHECK (state <> 'CANCELLED' OR cancelled_at IS NOT NULL)`
- Index `games_one_open`: `CREATE UNIQUE INDEX games_one_open ON games ((true)) WHERE state NOT IN ('CLOSED', 'CANCELLED')` (DEC-101; real or test)
- Index `games_code_idx` ON (`code`) (non-unique)
- Index `games_past_idx` ON (`closed_at DESC`) WHERE `state = 'CLOSED' AND NOT is_test`
- Index `games_results_idx` ON (`results_at`) WHERE `state = 'RESULTS'`

Table `top_ten_entries` (7.6, 10.1; DEC-39, DEC-45):

- `game_id`: uuid NOT NULL REFERENCES `games (id)` ON DELETE CASCADE
- `sort_order`: smallint NOT NULL; CHECK `>= 1`; stable display order under shared ranks
- `player_rank`: smallint NOT NULL; CHECK BETWEEN 1 AND 10 (BR-09)
- `player_name`: varchar(20) NOT NULL; CHECK `char_length >= 1`
- `points`: integer NOT NULL; may be negative (DEC-23)
- PRIMARY KEY (`game_id`, `sort_order`)

Migration files (10; in `backend/src/main/resources/db/migration`):

- `V1__create_schema.sql`: all six tables, constraints and indexes above (10.1)
- `V2__default_characters.sql`: inserts the four characters (10.2)

Default characters from V2 (10.2; seed loader later updates by role):

- MANAGER: Maya, "Quick one!"; correct "Client's happy. You're a legend.", "That's going in my good-news update.", "Nailed it. Coffee's on me."; wrong "That's going in my status report.", "The client just called. Again.", "Let's take that one offline."
- BUSINESS_ANALYST: Ben, "What exactly do we mean by fast?"; correct "Crystal clear. I'm framing that answer.", "That's exactly what the user story meant!", "Requirements understood. Chef's kiss."; wrong "Hmm, that's not what the user story says.", "Let's revisit the acceptance criteria.", "Adding that to my list of questions."
- DEVELOPER: Dev, "Works on my machine."; correct "Merged. No conflicts.", "Clean build. Beautiful.", "Ship it!"; wrong "That broke the build.", "Merge conflict incoming.", "Who wrote this? Oh. Me."
- TESTER: Tess, "Found another one!"; correct "Bug squashed!", "Test passed. I'm almost disappointed.", "Zero defects. Suspicious, but nice."; wrong "That bug just reached production.", "Reopening the ticket.", "Logged it. Severity: ouch."
- SQL escapes apostrophes as `''`.

## What implementation must do

Conventions (4): PostgreSQL 18 (DEC-147) via Spring Data JPA/Hibernate; Flyway owns all schema changes; `snake_case` columns, plural tables; UUID PKs assigned by the application (DB-02); every timestamp `timestamptz` in UTC; enums stored as text with CHECK matching Java enum names in LLD 5.2; avoid SQL keywords (`task_type`, `sort_order`, `player_rank`, `is_test`).

JSON formats (8; all validated by `ContentValidator` before write, LLD 5.3):

- Character lines: array of exactly 3 strings (8.1).
- Code snippet: `{ "language": ..., "text": ... }`; `language` one of text, java, javascript, typescript, sql, json, python, shell; `text` at most 2,000 characters and 30 lines (SRS 7.3) (8.2).
- MULTIPLE_CHOICE: `options` 2–4 objects `{text (1–80), correct}`; exactly one correct; stored in display order (8.3).
- YES_NO: `{"answerYes": true|false}` (8.3).
- ORDER: `items` 3–5 objects `{text (1–60), correctPosition (1..n, each once)}`; stored in display order, which must differ from correct order (8.3).
- PROBLEM_WORDS: `markedText` 1–200 chars with 1–4 words marked `{{like this}}`, each marker wrapping one whole word; `monospace` boolean (DB-06) (8.3).
- Seed-to-DB mapping (8.4): `key` to `tasks.task_key` or `run_plans.plan_key`; `role`, `kind`, `phase`, `prompt`, `explanation` same names; `type` to `tasks.task_type`; `code` to `tasks.code`; `timeLimitSeconds` to `tasks.time_limit_seconds` (null when absent); `options` to `content.options`; `answer` ("YES"/"NO") to `content.answerYes`; `items` to `content.items`; `text` and optional `monospace` to `content.markedText`, `content.monospace` (false when absent); `roundLengthMinutes` to `run_plans.round_length_minutes`; `incident` to `run_plans.incident_task_id`; `practice`, `phases` to `run_plan_entries` rows with `sort_order` from list order; `displayName`, `introLine`, `correctLines`, `wrongLines` to `characters` columns.
- Game snapshot (8.5): keys `formatVersion` (1), `runPlanName`, `roundLengthSeconds` (e.g. 300), `characters` map by role with `displayName`, `introLine`, `correctLines`, `wrongLines`; `practice` array of tasks with `key`, `role`, `kind`, `type`, `prompt`, `code`, `timeLimitMs` (e.g. 8000), `content`, `explanation`; `incident` task object; `phases` map with `PLANNING`, `DEVELOPMENT`, `TESTING`, `RELEASE` arrays. All time limits resolved to milliseconds with defaults applied; every character included; includes correct answers; read only by the backend, never sent to clients (DEC-130); a 68-task snapshot is about 50–100 KB.

Integrity split (9): DB enforces enums, key formats, lengths, time-limit and round-length ranges (app also validates first for friendly messages), phase iff scored, incident is MC, one task per plan, unique positions (at commit), FK blocks deleting a used task (app lists plans, FR-071), one open game, projector key rules, required timestamps (FR-087, FR-088). Application enforces JSON shapes, exactly one correct option, display order different from correct order, 1–4 marked words (SRS 7.3); phase list holds only scored tasks of that phase, practice list only practice tasks, incident slot only an incident task (FR-076); valid state transitions via compare-and-set (SRS 3.1).

Hibernate: `spring.jpa.hibernate.ddl-auto=validate` (10, DB-06).

Migration rules (10.3): never edit an applied migration; one logical change per migration named `V<number>__<what_it_does>.sql`; every migration tested in the pipeline against a fresh PostgreSQL 18 Testcontainers container and against a copy of the previous schema; seed file loaded only by the seed command (DEC-136), never Flyway.

Write patterns (11):

- Edit task, character or run plan: JPA update with `version` check; mismatch returns `EDIT_CONFLICT` (DEC-144).
- Replace a run plan's lists: delete and re-insert entries in one transaction (deferred constraint).
- Create game: insert in CREATED; partial unique index rejects a second open game.
- State change: `UPDATE games SET state = :new, <time column> = now(), updated_at = now() WHERE id = :id AND state = :expected` on the state recorder thread (DB-05).
- Reach Results: one transaction, CAS to RESULTS with `results_at` and `player_count`, then insert top-10 rows (FR-087).
- Close: `UPDATE games SET state = 'CLOSED', closed_at = now(), projector_key = NULL, updated_at = now() WHERE id = :id AND state = 'RESULTS'`; zero rows means not allowed.
- Cancel: same pattern to CANCELLED from any state before RESULTS (FR-084).
- Delete test game: `DELETE FROM games WHERE id = :id AND is_test`; top-10 rows cascade.
- Start-up cleanup (FR-089): `UPDATE games SET state = 'CANCELLED', cancelled_at = now(), projector_key = NULL WHERE state IN ('LOBBY', 'PRACTICE', 'COUNTDOWN', 'LIVE', 'FROZEN', 'ENDED', 'REVEAL') AND NOT is_test`, and delete test games not in CREATED.

Queries and indexes (12): task by key (unique `task_key`; seed loader, snapshots); run plans using a task (`run_plan_entries_task_idx`; FR-071); the open game (`games_one_open`; joining, creation, deploy lock); game by join code (`games_code_idx`; FR-001); past games newest first (`games_past_idx`; FR-086); games waiting in Results (`games_results_idx`; housekeeping FR-088); task library filter (no index; FR-070).

Volumes and settings (13): tasks 100–500 rows; about one game per event; about 10 top-10 rows per game; snapshots 50–100 KB; DB well under 100 MB. `spring.datasource.hikari.maximum-pool-size=5`; `spring.jpa.open-in-view=false`; Hibernate JDBC time zone UTC; PostgreSQL `shared_buffers` 256 MB in a 1 GB container (DEC-150).

Security and retention (14): one non-superuser role used by app and Flyway, password from environment file, DB port only inside Compose network (DB-07); only personal data is display names in `top_ten_entries` (DEC-39); retention: content until admin deletes, games and top 10s kept, test games deleted (FR-085), projector keys cleared at close or cancel; nightly `pg_dump` custom format copied off machine, details in Deployment Guide (OI-07); restore documented and rehearsed before trial run (FR-093).

Test data (15): integration tests on PostgreSQL 18 Testcontainers with the same Flyway migrations; seed has 68 scored, 4 practice, 2 incident tasks, 4 characters, 2 run plans; 16 constraint tests existed during drafting (reject bad data, allow valid flows including reordering entries and close-then-open-new-game).

## Ordering and dependencies

- V1 before V2; V2 characters must exist before any task insert (FK `tasks.role`) (DB-03).
- Tasks before run plans (incident FK) and before `run_plan_entries`.
- Seed load happens after Flyway, through the seed command, updating characters by role (10.2, 10.3).
- A game needs a snapshot at creation (NOT NULL), so run plan, tasks and characters must be valid first (7.5, 8.5).
- Reach Results: CAS then top-10 inserts in the same transaction (11).
- Only one open game at a time, so a new game requires the previous one CLOSED or CANCELLED (index).
- Restore rehearsal before the trial run (14).
- DB design depends on SAD v1.0 and LLD v1.0; feeds API spec (11), test cases (15), deployment (16), setup (18).

## Dates and milestones

- Approved 23 September 2026 (v0.1 and v1.0 both 2026-09-23).
- Restore rehearsed before the trial run (FR-093) (14).
- No other dates.

## Owner-only actions

- Set the database role password in the production environment file (DB-07, 14).
- Choose backup frequency, off-machine target and retention (OI-07, Deployment Guide) and rehearse a restore before the trial run (14).
- Run the seed command with shell access (DEC-136).
- Decide whether to add past-game deletion later (17).

## Easy to get wrong

- `updated_at` has only a DEFAULT; no trigger updates it, so every UPDATE (JPA and CAS SQL) must set it explicitly.
- `ddl-auto=validate` means JPA mappings must match exactly: jsonb columns need JSON JDBC type mapping, smallint to `Short`, varchar types; any schema change requires a new Flyway migration.
- `run_plan_entries_order_unique` is DEFERRABLE INITIALLY DEFERRED: reorder by delete and re-insert in one transaction; Hibernate flush order does not matter but the transaction must commit atomically.
- Sort order starts at 0 in `run_plan_entries` but at 1 in `top_ten_entries`.
- `player_rank` 1–10 while the list can exceed 10 rows on ties; PK is (`game_id`, `sort_order`), not rank.
- `points` can be negative.
- `games_one_open` counts test games too: a test game in any non-final state blocks a real game.
- `games_code_idx` is not unique: codes can repeat across past games, so join lookups must also restrict to the open game.
- Projector key must be set at insert (CREATED needs it by `games_key_present_while_open`) and set to NULL in the same UPDATE that moves to CLOSED or CANCELLED, or the check fails.
- Moving to RESULTS or CLOSED without `results_at` violates the check; close SQL relies on it already being set.
- `phase` must be null for PRACTICE and INCIDENT; `time_limit_seconds` null means type default, resolved to ms only in the snapshot.
- Snapshot uses different units and names than tables (`roundLengthSeconds`, `timeLimitMs`, `type`, `key`); never send it to clients.
- Seed `answer` "YES"/"NO" becomes boolean `answerYes`; `monospace` defaults to false; the LLD field is `monospace`, not `code` (DB-06).
- Deleting a run plan sets `games.run_plan_id` to null (keep `run_plan_name` copy); deleting a task used as an incident or entry fails on FK, and the app must list the plans.
- Start-up cleanup applies only to non-test games in LOBBY..REVEAL; RESULTS survives restart (auto-close later) and CREATED is left alone.
- All game-row writes on the single state recorder thread (DB-05); never write game rows from Tomcat or session threads.
- Pool is 5 connections; open-in-view off, so lazy loading outside transactions fails.

## Doc issues noticed

- Section 11, start-up cleanup SQL omits `updated_at = now()`, unlike every other state change pattern. Suggested fix: add `updated_at = now()`.
- Section 11, start-up cleanup: real games in CREATED are neither cancelled nor mentioned, yet live state is in memory only; after a restart a CREATED game stays open and blocks `games_one_open`. Also "delete test games not in CREATED" deletes CLOSED/RESULTS test games but keeps CREATED ones. Suggested fix: state explicitly what happens to CREATED games (real and test) after a restart, cross-checked with FR-089 and the LLD.
- Section 12 "A game by join code" uses non-unique `games_code_idx` without saying the lookup is limited to the open game; code uniqueness among open games relies on `games_one_open`. Suggested fix: note the query predicate (`state NOT IN ('CLOSED','CANCELLED')`).
- Section 9 lists "Valid state transitions" as application-only, but section 11's Cancel says "from any state before RESULTS" without an explicit SQL predicate (a CAS on a single expected state). Suggested fix: give the exact cancel SQL, including `projector_key = NULL` and `cancelled_at`.
- Section 10.3 says each migration is tested "against a copy of the previous schema" without saying how that copy is produced in CI. Suggested fix: specify (for example, apply migrations up to N-1 in Testcontainers, then N).
- Section 6 ERD omits `lobby_opened_at`, `round_started_at`, `round_ended_at`, `cancelled_at` (acknowledged in text); acceptable but readers must use 7.5.
- Document control drafting note says the DDL was tested on PostgreSQL 16 while the design targets 18; low risk, but the 16 constraint tests should be ported into the pipeline (section 15 implies but does not name them as test cases).
- Section 14 notes a database role "used by the application and Flyway"; combined with DB-07 least privilege, that role still needs DDL rights. Not a contradiction, but "least-privilege" means no superuser only. Clarify.
