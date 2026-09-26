package app.deliveryhero.support;

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

    private TestData() {}
}
