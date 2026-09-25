package app.deliveryhero.content;

import java.util.List;

/** Multiple-choice options in the order players see them (LLD section 5.2). */
public record MultipleChoiceContent(List<Option> options) implements TaskContent {

    public record Option(String text, boolean correct) {}
}
