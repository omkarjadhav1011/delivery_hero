package app.deliveryhero.support;

import app.deliveryhero.config.GameProperties;
import app.deliveryhero.content.GameSnapshot;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Shared test data, named as in document 15's data sets (DS-02, DS-04), in one place for every test. */
public final class TestData {

    /** The join code of the game players join in examples and tests (DS-02; API section 7.2). */
    public static final String GAME_CODE = "K7PQ2M";

    public static final UUID GAME_ID = UUID.fromString("3f6c1a52-8d1e-4f1b-9a3c-2b7e5d9c0a11");

    /** A code in the BR-17 alphabet that no game has. */
    public static final String UNKNOWN_CODE = "ZZZZ22";

    /** DS-04: the name typed with extra spaces, and what it becomes. */
    public static final String PRIYA_TYPED = "  Priya   S ";

    public static final String PRIYA = "Priya S";

    /** A snapshot with no tasks, for sessions that only join and connect. */
    public static final GameSnapshot EMPTY_SNAPSHOT =
            new GameSnapshot(GameSnapshot.FORMAT_VERSION, "Test plan", 180, Map.of(), List.of(), null, Map.of());

    /** The production game settings of LLD section 5.13: a 5-second countdown and a 30-second freeze. */
    public static final GameProperties GAME_PROPERTIES = new GameProperties(
            100,
            Duration.ofSeconds(5),
            Duration.ofSeconds(30),
            Duration.ofSeconds(30),
            Duration.ofHours(24),
            Duration.ofHours(2),
            Duration.ofMinutes(3),
            null);

    private TestData() {}
}
