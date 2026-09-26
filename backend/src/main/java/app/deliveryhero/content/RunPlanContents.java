package app.deliveryhero.content;

import app.deliveryhero.common.Phase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A stored run plan with its lists read back as task definitions, each in play order (document 10, sections 7.3 and
 * 7.4).
 *
 * @param incident the incident task, or null when the plan has none
 * @param phases every phase, with an empty list for a phase without tasks
 */
public record RunPlanContents(
        UUID id,
        String key,
        String name,
        int roundLengthMinutes,
        int version,
        List<TaskDefinition> practice,
        @Nullable TaskDefinition incident,
        Map<Phase, List<TaskDefinition>> phases) {}
