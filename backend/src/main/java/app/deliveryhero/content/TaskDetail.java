package app.deliveryhero.content;

import app.deliveryhero.common.Phase;
import app.deliveryhero.common.Role;
import app.deliveryhero.common.TaskKind;
import app.deliveryhero.common.TaskType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** A task as admins see it, correct answers included (API section 7.4; DI-11: admins only). */
public record TaskDetail(
        UUID id,
        String key,
        Role role,
        TaskKind kind,
        @Nullable Phase phase,
        TaskType type,
        String prompt,
        @Nullable CodeSnippet code,
        @Nullable Integer timeLimitSeconds,
        int effectiveTimeLimitSeconds,
        TaskContent content,
        @Nullable String explanation,
        int version,
        List<PlanReference> usedBy,
        Instant createdAt,
        Instant updatedAt,
        List<Issue> warnings) {

    /** A run plan that uses the task. */
    public record PlanReference(UUID id, String name) {}
}
