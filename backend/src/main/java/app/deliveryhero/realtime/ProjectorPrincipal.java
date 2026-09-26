package app.deliveryhero.realtime;

import java.util.UUID;

/** The projector of one game: display only, it may send nothing but time-sync requests (FR-052, LD-02). */
public record ProjectorPrincipal(UUID gameId) implements ClientPrincipal {

    @Override
    public String getName() {
        return "projector:" + gameId;
    }
}
