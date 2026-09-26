package app.deliveryhero.content;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A character update from the admin API (API section 7.5). Anything may be missing; {@link CharacterService} reports
 * what is. The role comes from the path.
 */
public record CharacterInput(
        @Nullable String displayName,
        @Nullable String introLine,
        @Nullable List<String> correctLines,
        @Nullable List<String> wrongLines,
        @Nullable Integer version) {}
