package app.deliveryhero.content;

/** Text with its problem words marked {@code {{like this}}} (LLD section 5.2). */
public record ProblemWordsContent(String markedText, boolean monospace) implements TaskContent {}
