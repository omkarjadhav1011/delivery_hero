package app.deliveryhero.api.pub;

import org.jspecify.annotations.Nullable;

/** The name as the player typed it (API section 7.2); the server tidies and checks it (BR-16). */
public record JoinRequest(@Nullable String name) {

    /** Leaves out the name, which never goes into logs (DEC-104). */
    @Override
    public String toString() {
        return "JoinRequest[]";
    }
}
