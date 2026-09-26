package app.deliveryhero.engine.command;

import app.deliveryhero.common.AnswerPayload;
import java.time.Instant;
import java.util.UUID;

/** A player's answer, stamped by the gateway when it arrived (LLD sections 5.4.3 and 5.6). */
public record SubmitAnswer(UUID playerId, String taskKey, AnswerPayload answer, Instant receivedAt)
        implements Command {}
