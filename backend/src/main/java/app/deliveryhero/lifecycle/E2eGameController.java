package app.deliveryhero.lifecycle;

import app.deliveryhero.common.GameState;
import app.deliveryhero.common.Ids;
import app.deliveryhero.engine.GameEngine;
import java.security.SecureRandom;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test scaffolding for the walking skeleton's end-to-end tests, in the {@code e2e} profile only (DI-08, DI-24). It
 * opens a fresh game in LOBBY with code K7PQ2M, discarding any game with that code first, so a retried or repeated
 * spec still joins as exactly "Priya". The game lives only in memory: no {@code games} row, so the deploy lock and
 * the seed command are unaffected. TODO(US-59): delete it once {@code POST /api/admin/games} creates games (S1-04 T6).
 */
@RestController
@Profile("e2e")
public class E2eGameController {

    /** The path, opened in {@code SecurityConfig} only when the {@code e2e} profile is active. */
    public static final String PATH = "/api/test/s0-game";

    /** The code of DS-02 that the end-to-end specs join. */
    static final String CODE = "K7PQ2M";

    private final GameEngine engine;
    private final SecureRandom random;

    E2eGameController(GameEngine engine, SecureRandom random) {
        this.engine = engine;
        this.random = random;
    }

    /** The game the specs join. TODO(US-59): on the seed's Quick 3-minute plan once sessions carry a snapshot. */
    public record OpenedGame(UUID gameId, String code) {}

    @PostMapping(PATH)
    @ResponseStatus(HttpStatus.CREATED)
    public OpenedGame open() {
        engine.findByCode(CODE).ifPresent(old -> engine.discard(old.id()));
        UUID gameId = Ids.newUuid(random);
        engine.create(gameId, CODE, GameState.LOBBY, false);
        return new OpenedGame(gameId, CODE);
    }
}
