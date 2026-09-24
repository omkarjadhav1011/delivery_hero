-- Delivery Hero schema, version 1 (Database Design Document, section 10)

CREATE TABLE characters (
    role            varchar(20)  PRIMARY KEY
                    CHECK (role IN ('MANAGER', 'BUSINESS_ANALYST', 'DEVELOPER', 'TESTER')),
    display_name    varchar(20)  NOT NULL CHECK (char_length(display_name) >= 1),
    intro_line      varchar(80)  NOT NULL CHECK (char_length(intro_line) >= 1),
    correct_lines   jsonb        NOT NULL
                    CHECK (jsonb_typeof(correct_lines) = 'array' AND jsonb_array_length(correct_lines) = 3),
    wrong_lines     jsonb        NOT NULL
                    CHECK (jsonb_typeof(wrong_lines) = 'array' AND jsonb_array_length(wrong_lines) = 3),
    version         integer      NOT NULL DEFAULT 0,
    updated_at      timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE tasks (
    id                  uuid         PRIMARY KEY,
    task_key            varchar(40)  NOT NULL UNIQUE CHECK (task_key ~ '^[a-z0-9-]{1,40}$'),
    role                varchar(20)  NOT NULL REFERENCES characters (role),
    kind                varchar(10)  NOT NULL CHECK (kind IN ('SCORED', 'PRACTICE', 'INCIDENT')),
    phase               varchar(12)  CHECK (phase IN ('PLANNING', 'DEVELOPMENT', 'TESTING', 'RELEASE')),
    task_type           varchar(16)  NOT NULL
                        CHECK (task_type IN ('MULTIPLE_CHOICE', 'YES_NO', 'ORDER', 'PROBLEM_WORDS')),
    prompt              varchar(200) NOT NULL CHECK (char_length(prompt) >= 1),
    code                jsonb        CHECK (code IS NULL OR jsonb_typeof(code) = 'object'),
    time_limit_seconds  smallint     CHECK (time_limit_seconds BETWEEN 5 AND 60),
    content             jsonb        NOT NULL CHECK (jsonb_typeof(content) = 'object'),
    explanation         varchar(300),
    version             integer      NOT NULL DEFAULT 0,
    created_at          timestamptz  NOT NULL DEFAULT now(),
    updated_at          timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT tasks_phase_matches_kind CHECK ((kind = 'SCORED') = (phase IS NOT NULL)),
    CONSTRAINT tasks_incident_is_multiple_choice CHECK (kind <> 'INCIDENT' OR task_type = 'MULTIPLE_CHOICE')
);

CREATE TABLE run_plans (
    id                    uuid         PRIMARY KEY,
    plan_key              varchar(40)  NOT NULL UNIQUE CHECK (plan_key ~ '^[a-z0-9-]{1,40}$'),
    name                  varchar(60)  NOT NULL CHECK (char_length(name) >= 1),
    round_length_minutes  smallint     NOT NULL CHECK (round_length_minutes BETWEEN 3 AND 10),
    incident_task_id      uuid         REFERENCES tasks (id),
    version               integer      NOT NULL DEFAULT 0,
    created_at            timestamptz  NOT NULL DEFAULT now(),
    updated_at            timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE run_plan_entries (
    run_plan_id  uuid         NOT NULL REFERENCES run_plans (id) ON DELETE CASCADE,
    task_id      uuid         NOT NULL REFERENCES tasks (id),
    list_name    varchar(12)  NOT NULL
                 CHECK (list_name IN ('PRACTICE', 'PLANNING', 'DEVELOPMENT', 'TESTING', 'RELEASE')),
    sort_order   smallint     NOT NULL CHECK (sort_order >= 0),
    PRIMARY KEY (run_plan_id, task_id),
    CONSTRAINT run_plan_entries_order_unique UNIQUE (run_plan_id, list_name, sort_order)
        DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX run_plan_entries_task_idx ON run_plan_entries (task_id);

CREATE TABLE games (
    id                    uuid         PRIMARY KEY,
    code                  varchar(6)   NOT NULL CHECK (code ~ '^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{6}$'),
    projector_key         varchar(22),
    state                 varchar(10)  NOT NULL
                          CHECK (state IN ('CREATED', 'LOBBY', 'PRACTICE', 'COUNTDOWN', 'LIVE', 'FROZEN',
                                           'ENDED', 'REVEAL', 'RESULTS', 'CLOSED', 'CANCELLED')),
    is_test               boolean      NOT NULL DEFAULT false,
    run_plan_id           uuid         REFERENCES run_plans (id) ON DELETE SET NULL,
    run_plan_name         varchar(60)  NOT NULL,
    round_length_minutes  smallint     NOT NULL CHECK (round_length_minutes BETWEEN 3 AND 10),
    snapshot              jsonb        NOT NULL CHECK (jsonb_typeof(snapshot) = 'object'),
    player_count          smallint     CHECK (player_count BETWEEN 0 AND 100),
    created_at            timestamptz  NOT NULL DEFAULT now(),
    lobby_opened_at       timestamptz,
    round_started_at      timestamptz,
    round_ended_at        timestamptz,
    results_at            timestamptz,
    closed_at             timestamptz,
    cancelled_at          timestamptz,
    updated_at            timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT games_key_cleared_when_finished
        CHECK (projector_key IS NULL OR state NOT IN ('CLOSED', 'CANCELLED')),
    CONSTRAINT games_key_present_while_open
        CHECK (projector_key IS NOT NULL OR state IN ('CLOSED', 'CANCELLED')),
    CONSTRAINT games_results_time_set
        CHECK (state NOT IN ('RESULTS', 'CLOSED') OR results_at IS NOT NULL),
    CONSTRAINT games_closed_time_set CHECK (state <> 'CLOSED' OR closed_at IS NOT NULL),
    CONSTRAINT games_cancelled_time_set CHECK (state <> 'CANCELLED' OR cancelled_at IS NOT NULL)
);

-- DEC-101: at most one game, real or test, outside CLOSED and CANCELLED
CREATE UNIQUE INDEX games_one_open ON games ((true)) WHERE state NOT IN ('CLOSED', 'CANCELLED');
CREATE INDEX games_code_idx ON games (code);
CREATE INDEX games_past_idx ON games (closed_at DESC) WHERE state = 'CLOSED' AND NOT is_test;
CREATE INDEX games_results_idx ON games (results_at) WHERE state = 'RESULTS';

CREATE TABLE top_ten_entries (
    game_id      uuid         NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    sort_order   smallint     NOT NULL CHECK (sort_order >= 1),
    player_rank  smallint     NOT NULL CHECK (player_rank BETWEEN 1 AND 10),
    player_name  varchar(20)  NOT NULL CHECK (char_length(player_name) >= 1),
    points       integer      NOT NULL,
    PRIMARY KEY (game_id, sort_order)
);
