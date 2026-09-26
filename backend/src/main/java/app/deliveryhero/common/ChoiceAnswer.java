package app.deliveryhero.common;

/** A multiple-choice answer: the chosen option's 0-based display index. */
public record ChoiceAnswer(int optionIndex) implements AnswerPayload {}
