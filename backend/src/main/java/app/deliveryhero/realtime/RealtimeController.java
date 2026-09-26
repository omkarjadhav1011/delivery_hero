package app.deliveryhero.realtime;

import app.deliveryhero.engine.command.SubmitAnswer;
import app.deliveryhero.security.RateLimiter;
import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/** Messages that clients send over STOMP (LLD section 5.6); time sync joins it with S1-08. */
@Controller
public class RealtimeController {

    private final Clock clock;
    private final RateLimiter limiter;
    private final GameCommands commands;

    public RealtimeController(Clock clock, RateLimiter limiter, GameCommands commands) {
        this.clock = clock;
        this.limiter = limiter;
        this.commands = commands;
    }

    /**
     * Stamps the arrival time first, so a queue never costs a player time, then drops answers over
     * {@link RateLimiter#ANSWER} with no reply (API section 5.3, DI-18). {@link DestinationPolicy} lets only the
     * game's own players send here.
     */
    @MessageMapping("/games/{gameId}/answer")
    public void answer(@DestinationVariable UUID gameId, @Payload AnswerSubmit message, Principal principal) {
        Instant receivedAt = clock.instant();
        if (!(principal instanceof PlayerPrincipal player) || !player.gameId().equals(gameId)) {
            return;
        }
        if (!limiter.tryAcquire("answer:" + player.playerId(), RateLimiter.ANSWER)) {
            return;
        }
        commands.submit(new SubmitAnswer(player.playerId(), message.taskKey(), message.answer(), receivedAt));
    }
}
