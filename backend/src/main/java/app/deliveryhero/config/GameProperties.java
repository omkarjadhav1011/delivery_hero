package app.deliveryhero.config;

import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Game settings under {@code dh.game} (LLD section 5.13). The {@code e2e} profile shortens the timings for the
 * end-to-end tests (DEC-197).
 *
 * @param maxPlayers players allowed in one game (FR-006)
 * @param countdown the countdown before the round starts (FR-019)
 * @param practice the length of the practice round (FR-014)
 * @param freeze the leaderboard freeze and joining cutoff before the round ends (FR-050, FR-051)
 * @param autoClose how long after the round ends a game in Results is closed automatically (FR-088)
 * @param testRetention how long a test game stays in Results before it's deleted (FR-085)
 * @param minRoundLength the shortest allowed round: 3 minutes (FR-018), or 60 seconds in {@code e2e} (DEC-197)
 * @param randomSeed a fixed seed for the game's random generator, or null for a random one ({@code e2e} fixes it)
 */
@Validated
@ConfigurationProperties("dh.game")
public record GameProperties(
        @DefaultValue("100") @Positive int maxPlayers,
        @DefaultValue("5s") Duration countdown,
        @DefaultValue("30s") Duration practice,
        @DefaultValue("30s") Duration freeze,
        @DefaultValue("24h") Duration autoClose,
        @DefaultValue("2h") Duration testRetention,
        @DefaultValue("3m") Duration minRoundLength,
        @Nullable Long randomSeed) {}
