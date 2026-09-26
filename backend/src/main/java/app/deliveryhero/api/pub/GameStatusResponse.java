package app.deliveryhero.api.pub;

import app.deliveryhero.common.GameState;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** What the join screen shows before a name is typed (API section 7.2). {@code reason} is null when joinable. */
public record GameStatusResponse(
        UUID gameId,
        String code,
        GameState state,
        boolean test,
        boolean joinable,
        @Nullable NotJoinableReason reason) {}
