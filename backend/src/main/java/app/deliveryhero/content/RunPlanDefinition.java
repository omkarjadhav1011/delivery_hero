package app.deliveryhero.content;

import app.deliveryhero.common.Phase;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** A run plan as it is validated and saved, with its lists as task keys in play order. */
public record RunPlanDefinition(
        String key,
        String name,
        int roundLengthMinutes,
        List<String> practice,
        @Nullable String incident,
        Map<Phase, List<String>> phases) {}
