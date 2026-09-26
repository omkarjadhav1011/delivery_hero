package app.deliveryhero.realtime;

import app.deliveryhero.common.AnswerPayload;

/** ANSWER_SUBMIT from a phone to {@code /app/games/{gameId}/answer} (API section 8.4). */
public record AnswerSubmit(String taskKey, AnswerPayload answer) {}
