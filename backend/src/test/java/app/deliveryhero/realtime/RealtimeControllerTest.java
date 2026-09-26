package app.deliveryhero.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.common.ChoiceAnswer;
import app.deliveryhero.engine.command.Command;
import app.deliveryhero.engine.command.SubmitAnswer;
import app.deliveryhero.security.RateLimiter;
import app.deliveryhero.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The answer mapping: stamp the time, apply the per-player limit, then queue SubmitAnswer (LLD section 5.6). */
class RealtimeControllerTest {

    private static final UUID GAME = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final PlayerPrincipal PRIYA =
            new PlayerPrincipal(GAME, UUID.fromString("00000000-0000-4000-8000-0000000000a1"));
    private static final PlayerPrincipal ARJUN =
            new PlayerPrincipal(GAME, UUID.fromString("00000000-0000-4000-8000-0000000000a2"));
    private static final AnswerSubmit ANSWER = new AnswerSubmit("ba-plan-01", new ChoiceAnswer(2));

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-21T10:00:00Z"));
    private final List<Command> queued = new ArrayList<>();
    private final RealtimeController controller = new RealtimeController(clock, new RateLimiter(clock), queued::add);

    private List<SubmitAnswer> answersOf(PlayerPrincipal player) {
        return queued.stream()
                .map(SubmitAnswer.class::cast)
                .filter(answer -> answer.playerId().equals(player.playerId()))
                .toList();
    }

    @Test
    @DisplayName("AC-EN06-03 rate limits: the 6th answer in a second is dropped and another player is unaffected")
    void sixthAnswerInASecondIsDropped() {
        for (int attempt = 0; attempt < 6; attempt++) {
            controller.answer(GAME, ANSWER, PRIYA);
        }
        controller.answer(GAME, ANSWER, ARJUN);

        assertThat(answersOf(PRIYA)).hasSize(5);
        assertThat(answersOf(ARJUN)).hasSize(1);
    }

    @Test
    @DisplayName("An answer is stamped with the time it arrived and queued with its task and payload")
    void answerIsStampedAndQueued() {
        clock.advance(Duration.ofMillis(1234));

        controller.answer(GAME, ANSWER, PRIYA);

        assertThat(queued)
                .containsExactly(new SubmitAnswer(
                        PRIYA.playerId(),
                        "ba-plan-01",
                        new ChoiceAnswer(2),
                        Instant.parse("2026-10-21T10:00:01.234Z")));
    }

    @Test
    @DisplayName("Answers pass again once the second has passed")
    void limitResetsAfterASecond() {
        for (int attempt = 0; attempt < 6; attempt++) {
            controller.answer(GAME, ANSWER, PRIYA);
        }
        clock.advance(Duration.ofSeconds(1));

        controller.answer(GAME, ANSWER, PRIYA);

        assertThat(answersOf(PRIYA)).hasSize(6);
    }
}
