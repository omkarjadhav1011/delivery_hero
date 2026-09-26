package app.deliveryhero.content;

import app.deliveryhero.common.Phase;
import app.deliveryhero.common.Role;
import app.deliveryhero.common.TaskKind;
import app.deliveryhero.common.TaskType;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** A row of the task library: no answers, and how many run plans use the task (API section 7.4, FR-070). */
public record TaskSummary(
        UUID id,
        String key,
        Role role,
        TaskKind kind,
        @Nullable Phase phase,
        TaskType type,
        String prompt,
        int effectiveTimeLimitSeconds,
        int usedByCount,
        int version) {}
