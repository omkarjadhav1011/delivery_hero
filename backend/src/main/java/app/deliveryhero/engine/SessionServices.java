package app.deliveryhero.engine;

import app.deliveryhero.broadcast.Broadcaster;
import app.deliveryhero.common.TokenService;
import app.deliveryhero.config.GameProperties;
import app.deliveryhero.engine.timer.TimerScheduler;
import app.deliveryhero.lifecycle.GameStateRecorder;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.random.RandomGenerator;

/**
 * What every session shares: settings, the broadcaster, the timers and the state recorder. All of it is safe to call
 * from any session thread.
 *
 * @param batchInterval the {@code FLUSH} period, {@code dh.broadcast.batch-interval}
 * @param random secure randomness for player IDs and tokens
 * @param gameRandom the game's generator for the incident moment, seeded in the e2e profile (DEC-197)
 */
record SessionServices(
        GameProperties properties,
        Duration batchInterval,
        TokenService tokens,
        PlayerTokens playerTokens,
        Broadcaster broadcaster,
        TimerScheduler timers,
        GameStateRecorder recorder,
        Clock clock,
        SecureRandom random,
        RandomGenerator gameRandom) {}
