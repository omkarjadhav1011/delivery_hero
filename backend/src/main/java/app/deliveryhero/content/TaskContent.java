package app.deliveryhero.content;

/** Type-specific task content, stored as JSON (LLD section 5.2, DEC-131). */
public sealed interface TaskContent permits MultipleChoiceContent, YesNoContent, OrderContent, ProblemWordsContent {}
