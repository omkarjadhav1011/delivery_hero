package app.deliveryhero.common;

import java.util.Set;

/** A problem-words answer: the selected token indexes. */
public record WordsAnswer(Set<Integer> tokenIndexes) implements AnswerPayload {}
