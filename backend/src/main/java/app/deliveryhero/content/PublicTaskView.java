package app.deliveryhero.content;

import app.deliveryhero.common.Role;
import app.deliveryhero.common.TaskType;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The only task shape that leaves the server before Results (LLD section 5.3, DEC-130, API section 9.1): the phones
 * and the editor's preview both get it from {@link #of}, and it never carries correct-answer data (NFR-12).
 */
public record PublicTaskView(
        String key,
        TaskType type,
        Role role,
        String characterName,
        String prompt,
        @Nullable CodeSnippet code,
        int timeLimitMs,
        @Nullable List<String> options,
        @Nullable List<String> items,
        @Nullable List<String> tokens,
        @Nullable Boolean monospace) {

    public static PublicTaskView of(TaskDefinition task, String characterName) {
        List<String> options = null;
        List<String> items = null;
        List<String> tokens = null;
        Boolean monospace = null;
        switch (task.content()) {
            case MultipleChoiceContent c ->
                options = c.options().stream()
                        .map(MultipleChoiceContent.Option::text)
                        .toList();
            case YesNoContent c -> {}
            case OrderContent c ->
                items = c.items().stream().map(OrderContent.Item::text).toList();
            case ProblemWordsContent c -> {
                tokens = tokens(c.markedText());
                monospace = c.monospace();
            }
        }
        return new PublicTaskView(
                task.key(),
                task.type(),
                task.role(),
                characterName,
                task.prompt(),
                task.code(),
                task.effectiveTimeLimitSeconds() * 1000,
                options,
                items,
                tokens,
                monospace);
    }

    /** The text split at whitespace with the {@code {{ }}} markers removed; which tokens were marked stays private. */
    private static List<String> tokens(String markedText) {
        String trimmed = markedText.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        return List.of(trimmed.split("\\s+")).stream()
                .map(token -> token.replace("{{", "").replace("}}", ""))
                .toList();
    }
}
