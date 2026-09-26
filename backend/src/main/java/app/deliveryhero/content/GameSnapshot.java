package app.deliveryhero.content;

import app.deliveryhero.common.Phase;
import app.deliveryhero.common.Role;
import app.deliveryhero.common.TaskKind;
import app.deliveryhero.common.TaskType;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A game's own copy of its run plan, tasks and characters, taken when the game is created so later edits never change
 * it (document 10, section 8.5; FR-072, DEC-100). It holds correct answers, so it stays in the backend and is never
 * sent to a client (DEC-130).
 *
 * @param formatVersion the snapshot format, {@link #FORMAT_VERSION}
 * @param roundLengthSeconds the round length, in seconds
 * @param incident the incident task, or null when the plan has none
 * @param phases every phase, each list in play order (empty lists included)
 */
public record GameSnapshot(
        int formatVersion,
        String runPlanName,
        int roundLengthSeconds,
        Map<Role, Character> characters,
        List<Task> practice,
        @Nullable Task incident,
        Map<Phase, List<Task>> phases) {

    public static final int FORMAT_VERSION = 1;

    public GameSnapshot {
        characters = Map.copyOf(characters);
        practice = List.copyOf(practice);
        phases = Map.copyOf(phases);
    }

    /** A character's name and reaction lines. */
    public record Character(String displayName, String introLine, List<String> correctLines, List<String> wrongLines) {

        public Character {
            correctLines = List.copyOf(correctLines);
            wrongLines = List.copyOf(wrongLines);
        }
    }

    /**
     * A task with its time limit resolved to milliseconds, defaults applied (DEC-74).
     *
     * @param content the type-specific content, correct answers included
     */
    public record Task(
            String key,
            Role role,
            TaskKind kind,
            TaskType type,
            String prompt,
            @Nullable CodeSnippet code,
            int timeLimitMs,
            TaskContent content,
            @Nullable String explanation) {}
}
