package app.deliveryhero.api.ops;

import org.jspecify.annotations.Nullable;

/**
 * The deploy-lock response, for example {@code {"locked": true, "state": "LIVE"}} (document 11, section 7.10).
 *
 * @param locked whether a game is between LOBBY and REVEAL (DEC-103)
 * @param state the open game's state, or null when no game is open
 */
public record DeployLockResponse(boolean locked, @Nullable String state) {

    /** No game is open. */
    public static final DeployLockResponse UNLOCKED = new DeployLockResponse(false, null);
}
