package app.deliveryhero.realtime;

import app.deliveryhero.engine.command.SubmitAnswer;
import app.deliveryhero.security.RateLimiter;
import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

/** Messages that clients send over STOMP (LLD section 5.6): answers, and time-sync requests. */
@Controller
public class RealtimeController {

    private static final Logger log = LoggerFactory.getLogger(RealtimeController.class);

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
        // A message missing its fields never reaches the engine, which assumes they are present
        if (message.taskKey() == null || message.answer() == null) {
            return;
        }
        if (!limiter.tryAcquire("answer:" + player.playerId(), RateLimiter.ANSWER)) {
            return;
        }
        commands.submit(new SubmitAnswer(player.playerId(), message.taskKey(), message.answer(), receivedAt));
    }

    /**
     * Answers a time-sync request with the client's own send time and the server's time, so the client can estimate
     * its offset (DEC-95, DEC-129). Every client kind may ask, the projector too (DEC-140), and the engine never hears
     * of it. The reply goes only to the connection that asked; a request without {@code clientSentAt} gets none.
     */
    @MessageMapping("/time-sync")
    @SendToUser(destinations = "/queue/time-sync", broadcast = false)
    public @Nullable TimeSyncReply timeSync(@Payload TimeSyncRequest request) {
        long serverTime = clock.millis();
        Long clientSentAt = request.clientSentAt();
        return clientSentAt == null ? null : new TimeSyncReply("TIME_SYNC", serverTime, clientSentAt);
    }

    /**
     * Drops a message that isn't valid JSON for its destination, such as an ANSWER_SUBMIT, or has no body at all. Only the event is logged: the converter's message can
     * quote the answer, and the default handler would log it with the session ID (DEC-104).
     */
    @MessageExceptionHandler({MessageConversionException.class, MethodArgumentNotValidException.class})
    public void malformed() {
        log.atDebug().addKeyValue("event", "MESSAGE_MALFORMED").log("Malformed message dropped");
    }
}
