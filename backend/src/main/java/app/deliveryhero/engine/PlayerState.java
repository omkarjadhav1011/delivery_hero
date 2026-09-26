package app.deliveryhero.engine;

import java.util.UUID;

/**
 * One player in a live game (LLD section 5.4.1), owned by the session thread. Scores, streaks and tasks arrive with
 * their stories.
 */
final class PlayerState {

    private final UUID id;
    private final String name;
    private final String tokenHash;

    PlayerState(UUID id, String name, String tokenHash) {
        this.id = id;
        this.name = name;
        this.tokenHash = tokenHash;
    }

    UUID id() {
        return id;
    }

    String name() {
        return name;
    }

    String tokenHash() {
        return tokenHash;
    }
}
