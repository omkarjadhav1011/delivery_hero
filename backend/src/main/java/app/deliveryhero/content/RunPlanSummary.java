package app.deliveryhero.content;

import java.util.UUID;

/**
 * A run plan in the list (API section 7.6).
 *
 * @param errorCount the errors that refuse a game from this plan: FR-077's, until the readiness check (US-58)
 * @param warningCount readiness warnings; 0 until the readiness check (US-58)
 */
public record RunPlanSummary(
        UUID id,
        String key,
        String name,
        int roundLengthMinutes,
        int scoredTaskCount,
        int errorCount,
        int warningCount,
        int version) {}
