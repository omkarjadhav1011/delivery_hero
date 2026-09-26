package app.deliveryhero.lifecycle;

import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.DeliveryHeroException;
import app.deliveryhero.common.GameState;
import app.deliveryhero.common.Ids;
import app.deliveryhero.common.TokenService;
import app.deliveryhero.content.ContentValidator;
import app.deliveryhero.content.GameSnapshot;
import app.deliveryhero.content.RunPlanContents;
import app.deliveryhero.content.RunPlanService;
import app.deliveryhero.content.ValidationReport;
import app.deliveryhero.engine.GameEngine;
import app.deliveryhero.realtime.CredentialRegistry;
import app.deliveryhero.realtime.ProjectorPrincipal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

/** Creates games and reports the open one (LLD section 5.8). Closing, cancelling and cleanup come with US-62 to US-67. */
@Service
public class GameLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(GameLifecycleService.class);

    /** The states of a finished game; any other state is open, and only one game is open at a time (DEC-101). */
    private static final Set<GameState> FINISHED = EnumSet.of(GameState.CLOSED, GameState.CANCELLED);

    /** The join code alphabet, without I, O, 0 and 1 (BR-17). */
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final int CODE_LENGTH = 6;

    /** The partial unique index that allows one open game (document 10, section 11). */
    private static final String ONE_OPEN_GAME_INDEX = "games_one_open";

    private final GameRepository games;
    private final RunPlanService plans;
    private final ContentValidator validator;
    private final SnapshotFactory snapshots;
    private final GameEngine engine;
    private final CredentialRegistry credentials;
    private final TokenService tokens;
    private final JsonMapper json;
    private final Clock clock;
    private final SecureRandom random;

    GameLifecycleService(
            GameRepository games,
            RunPlanService plans,
            ContentValidator validator,
            SnapshotFactory snapshots,
            GameEngine engine,
            CredentialRegistry credentials,
            TokenService tokens,
            JsonMapper json,
            Clock clock,
            SecureRandom random) {
        this.games = games;
        this.plans = plans;
        this.validator = validator;
        this.snapshots = snapshots;
        this.engine = engine;
        this.credentials = credentials;
        this.tokens = tokens;
        this.json = json;
        this.clock = clock;
        this.random = random;
    }

    /**
     * Creates a game in CREATED from a run plan's snapshot, in one transaction; its session starts once the row is
     * committed. Refused with {@code ANOTHER_GAME_OPEN} while a game is open, {@code NOT_FOUND} for an unknown plan and
     * {@code VALIDATION_FAILED} for a plan that breaks FR-077.
     *
     * @param botCount simulated players for a test game; TODO(US-63): start them with the test game endpoint (S2-10)
     */
    @Transactional
    public GameDetails create(UUID runPlanId, boolean test, int botCount) {
        if (games.existsByStateNotIn(FINISHED)) {
            throw new DeliveryHeroException(ApiErrorCode.ANOTHER_GAME_OPEN);
        }
        RunPlanContents plan =
                plans.load(runPlanId).orElseThrow(() -> new DeliveryHeroException(ApiErrorCode.NOT_FOUND));
        ValidationReport report = validator.validateForGame(plan);
        if (report.hasErrors()) {
            throw new DeliveryHeroException(ApiErrorCode.VALIDATION_FAILED, null, report.errors());
        }
        GameSnapshot snapshot = snapshots.create(plan);
        String projectorKey = tokens.newToken();
        // Stored in microseconds, so the row reads back equal
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        GameEntity game = new GameEntity(
                Ids.newUuid(random),
                newCode(),
                projectorKey,
                test,
                plan.id(),
                plan.name(),
                plan.roundLengthMinutes(),
                json.writeValueAsString(snapshot),
                now);
        try {
            games.saveAndFlush(game);
        } catch (DataIntegrityViolationException refused) {
            if (isOneOpenGameIndex(refused)) {
                // Another admin's game was created in the meantime; the partial unique index refused this one
                throw new DeliveryHeroException(ApiErrorCode.ANOTHER_GAME_OPEN);
            }
            throw refused;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                engine.create(game.id(), game.code(), GameState.CREATED, test, snapshot);
                credentials.registerProjector(new ProjectorPrincipal(game.id()), projectorKey);
                log.atInfo()
                        .addKeyValue("event", "GAME_CREATED")
                        .addKeyValue("gameId", game.id())
                        .addKeyValue("state", GameState.CREATED)
                        .addKeyValue("test", test)
                        .log("Game created");
            }
        });
        return details(game);
    }

    /** The open game, real or test, or empty when every game is closed or cancelled. */
    @Transactional(readOnly = true)
    public Optional<GameDetails> current() {
        return games.findFirstByStateNotIn(FINISHED).map(this::details);
    }

    private GameDetails details(GameEntity game) {
        String projectorKey = game.projectorKey();
        if (projectorKey == null) {
            throw new IllegalStateException("An open game always has a projector key (games_key_present_while_open)");
        }
        boolean lostOnRestart =
                game.state() == GameState.RESULTS && engine.find(game.id()).isEmpty();
        return new GameDetails(
                game.id(),
                game.code(),
                game.state(),
                game.test(),
                game.runPlanName(),
                game.roundLengthMinutes(),
                projectorKey,
                game.createdAt(),
                !lostOnRestart);
    }

    private static boolean isOneOpenGameIndex(DataIntegrityViolationException refused) {
        for (Throwable cause = refused; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return ONE_OPEN_GAME_INDEX.equals(violation.getConstraintName());
            }
        }
        return false;
    }

    /** A join code from the BR-17 alphabet that no stored game has used. */
    private String newCode() {
        String code;
        do {
            StringBuilder next = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                next.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            code = next.toString();
        } while (games.existsByCode(code));
        return code;
    }
}
