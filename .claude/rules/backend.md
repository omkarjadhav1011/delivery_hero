---
paths:
  - "backend/**"
---

# Backend rules (Java 21, Spring Boot 4.1)

Full rules: document 13, section 6; design: LLD section 5.

- Keep LLD section 5.1's packages and dependency rules; ArchUnit tests enforce them. `engine` never depends on `api` or repositories, `scoring` depends only on `common`, and nothing depends on `api`.
- Every package has `@NullMarked` in `package-info.java`; mark what may be null with `@Nullable`. NullAway checks `engine` and `scoring`, and Error Prone runs on every compile: fix findings rather than suppress them.
- Records for DTOs, commands, messages and properties; sealed interfaces for closed families, switched on without `default`.
- Only `config` may call `Instant.now()`, `System.currentTimeMillis()`, `new Random()` or `Math.random()`; everything else takes the injected `Clock` and generator (LLD section 4).
- A game's state is touched only on its own session thread: no locks, `synchronized` or atomics in `engine`, no blocking I/O or `Thread.sleep` there, and timers are commands on the queue (document 13, section 6.5).
- Answers are checked and scored on the server. No message to a phone carries correct answers before the round ends.
- Logs never contain player names, answers, passwords, player tokens, projector keys or session IDs; log IDs and counts (DEC-104).
- Constructor injection only; `@ConfigurationProperties` records with `@Validated`; scoring values live only in `scoring.yml`. Map environment variables explicitly in `application.yml`, for example `dh.admin.password-hash: ${DH_ADMIN_PASSWORD_HASH:}`.
- Controllers stay thin and never return entities; errors are a `DeliveryHeroException` with an `ApiErrorCode`, turned into Problem Details in one place.
- Migrations are new files only, `V<n>__<description>.sql`. Never edit a committed one, and keep each compatible with the previous release's code (document 16, section 10.3).
- A `TODO` references a GitHub issue: `// TODO(#42): ...`. Until issues exist, scaffold stubs may cite the story instead: `// TODO(US-56): ...`.
- Tests: `*Test` for unit tests (Surefire), `*IT` for integration tests with Testcontainers (Failsafe). A test for an acceptance criterion starts its `@DisplayName` with the ID. Given, when, then; a test clock and seeded generator; no `Thread.sleep`. Place tests where document 15, section 8.1, maps them.
- An API change also updates document 11, the frontend types in `frontend/src/types`, `contracts/` (rewritten by the contract test) and `docs/openapi.json` (copied from `backend/target/openapi.json` after the OpenAPI check).
- Before a pull request: `./mvnw spotless:apply`, then `./mvnw -B verify`.
