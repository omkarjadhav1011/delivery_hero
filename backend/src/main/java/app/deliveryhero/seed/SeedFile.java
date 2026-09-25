package app.deliveryhero.seed;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The seed file, format version 1, as written (SRS section 7.4). Every field may be missing or wrong, so the importer
 * can report each problem with its key instead of failing on the first one.
 */
public record SeedFile(
        @Nullable Integer formatVersion,
        @Nullable List<Character> characters,
        @Nullable List<Task> tasks,
        @Nullable List<RunPlan> runPlans) {

    public record Character(
            @Nullable String role,
            @Nullable String displayName,
            @Nullable String introLine,
            @Nullable List<String> correctLines,
            @Nullable List<String> wrongLines) {}

    public record Task(
            @Nullable String key,
            @Nullable String role,
            @Nullable String kind,
            @Nullable String phase,
            @Nullable String type,
            @Nullable String prompt,
            @Nullable Code code,
            @Nullable BigDecimal timeLimitSeconds,
            @Nullable List<Option> options,
            @Nullable String answer,
            @Nullable List<Item> items,
            @Nullable String text,
            @Nullable Boolean monospace,
            @Nullable String explanation) {}

    public record Code(@Nullable String language, @Nullable String text) {}

    public record Option(@Nullable String text, @Nullable Boolean correct) {}

    public record Item(@Nullable String text, @Nullable Integer correctPosition) {}

    public record RunPlan(
            @Nullable String key,
            @Nullable String name,
            @Nullable BigDecimal roundLengthMinutes,
            @Nullable List<String> practice,
            @Nullable String incident,
            @Nullable Map<String, List<String>> phases) {}
}
