package app.deliveryhero.content;

import app.deliveryhero.common.Phase;
import app.deliveryhero.common.Role;
import app.deliveryhero.common.TaskKind;
import app.deliveryhero.common.TaskType;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The task input of the admin API, for create, update and public view (API section 7.4). Anything may be missing;
 * {@link TaskService} reports what is, and reads {@code content} in the format of {@code type} (document 10, 8.3).
 */
public record TaskInput(
        @Nullable String key,
        @Nullable Role role,
        @Nullable TaskKind kind,
        @Nullable Phase phase,
        @Nullable TaskType type,
        @Nullable String prompt,
        @Nullable CodeSnippet code,
        @Nullable Integer timeLimitSeconds,
        @Nullable Map<String, Object> content,
        @Nullable String explanation,
        @Nullable Integer version) {}
