package app.deliveryhero.content;

/** The answer to a yes/no statement (LLD section 5.2). */
public record YesNoContent(boolean answerYes) implements TaskContent {}
