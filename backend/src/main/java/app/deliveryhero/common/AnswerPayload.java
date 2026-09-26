package app.deliveryhero.common;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** What a player submits, told apart on the wire by {@code kind} (LLD section 5.2, API section 8.4). */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ChoiceAnswer.class, name = "CHOICE"),
    @JsonSubTypes.Type(value = YesNoAnswer.class, name = "YES_NO"),
    @JsonSubTypes.Type(value = OrderAnswer.class, name = "ORDER"),
    @JsonSubTypes.Type(value = WordsAnswer.class, name = "WORDS")
})
public sealed interface AnswerPayload permits ChoiceAnswer, YesNoAnswer, OrderAnswer, WordsAnswer {}
