package app.deliveryhero.engine.command;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.ChoiceAnswer;
import app.deliveryhero.common.WordsAnswer;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A logged command or result never shows a name, a token, a token hash or a connection ID (DEC-104). */
class CommandLoggingTest {

    private static final String TOKEN_HASH = "hash-of-a-player-token";
    private static final String CONNECTION = "connection-7f3a";

    @Test
    @DisplayName("Gateway commands print neither the token hash nor the connection ID")
    void commandsHideSecretsAndIds() {
        UUID player = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

        assertThat(new Reconnect(TOKEN_HASH, CONNECTION).toString()).isEqualTo("Reconnect[]");
        assertThat(new Disconnect(CONNECTION).toString()).isEqualTo("Disconnect[]");
        assertThat(new ClientSubscribed(CONNECTION, ClientRole.PLAYER, player, null).toString())
                .isEqualTo("ClientSubscribed[role=PLAYER, playerId=" + player + ", gameId=null]")
                .doesNotContain(CONNECTION);
    }

    @Test
    @DisplayName("SubmitAnswer prints neither the answer nor its arrival time")
    void submitAnswerHidesTheAnswer() {
        UUID player = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

        assertThat(new SubmitAnswer(player, "T-01", new WordsAnswer(Set.of(4217)), Instant.EPOCH).toString())
                .isEqualTo("SubmitAnswer[playerId=" + player + ", taskKey=T-01]")
                .doesNotContain("4217");
    }

    @Test
    @DisplayName("Join and its result print neither the name nor the token")
    void joinHidesNameAndToken() {
        UUID game = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
        UUID player = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

        assertThat(new Join("Priya S", new CompletableFuture<>()).toString()).isEqualTo("Join[]");
        assertThat(new JoinResult.Joined(game, player, "Priya S", "q3Xk9vT2bLmN8pR4sW7yZa").toString())
                .isEqualTo("Joined[gameId=" + game + ", playerId=" + player + "]");
        assertThat(new JoinResult.Refused(ApiErrorCode.INVALID_NAME).toString()).doesNotContain("Priya");
    }

    @Test
    @DisplayName("RenamePlayer prints the player's ID but not the new name")
    void renameHidesName() {
        UUID player = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

        assertThat(new RenamePlayer(player, "Priya S", new CompletableFuture<>()).toString())
                .isEqualTo("RenamePlayer[playerId=" + player + "]");
    }

    @Test
    @DisplayName("SubmitAnswer prints the player and task but never the answer")
    void answerHidden() {
        UUID player = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

        assertThat(new SubmitAnswer(player, "DEV-01", new ChoiceAnswer(2), Instant.EPOCH).toString())
                .isEqualTo("SubmitAnswer[playerId=" + player + ", taskKey=DEV-01]");
    }
}
