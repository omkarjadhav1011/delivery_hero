package app.deliveryhero.content;

import app.deliveryhero.common.Phase;
import app.deliveryhero.common.Role;
import app.deliveryhero.common.TaskKind;
import app.deliveryhero.common.TaskType;
import org.jspecify.annotations.Nullable;

/** A task as it is validated and saved, whether it comes from the seed file or the admin panel. */
public record TaskDefinition(
        String key,
        Role role,
        TaskKind kind,
        @Nullable Phase phase,
        TaskType type,
        String prompt,
        @Nullable CodeSnippet code,
        @Nullable Integer timeLimitSeconds,
        TaskContent content,
        @Nullable String explanation) {

    /** The set limit, or the default for the task (DEC-74): incident tasks 20 seconds, whatever their type. */
    public int effectiveTimeLimitSeconds() {
        if (timeLimitSeconds != null) {
            return timeLimitSeconds;
        }
        if (kind == TaskKind.INCIDENT) {
            return 20;
        }
        return switch (type) {
            case MULTIPLE_CHOICE -> 15;
            case YES_NO -> 8;
            case ORDER -> 25;
            case PROBLEM_WORDS -> 20;
        };
    }
}
