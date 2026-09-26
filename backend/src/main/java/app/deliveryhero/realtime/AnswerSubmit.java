package app.deliveryhero.realtime;

import app.deliveryhero.common.AnswerPayload;
import org.jspecify.annotations.Nullable;

/** ANSWER_SUBMIT from a phone to {@code /app/games/{gameId}/answer} (API section 8.4); a phone may omit a field. */
public record AnswerSubmit(
        @Nullable String taskKey, @Nullable AnswerPayload answer) {}
