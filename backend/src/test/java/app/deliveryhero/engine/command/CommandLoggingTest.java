package app.deliveryhero.engine.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A logged command never shows a token hash or a connection ID (DEC-104). */
class CommandLoggingTest {

    private static final String TOKEN_HASH = "hash-of-a-player-token";
    private static final String CONNECTION = "connection-7f3a";

    @Test
    @DisplayName("Gateway commands print neither the token hash nor the connection ID")
    void commandsHideSecretsAndIds() {
        UUID player = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

        assertThat(new Reconnect(TOKEN_HASH, CONNECTION).toString()).isEqualTo("Reconnect[]");
        assertThat(new Disconnect(CONNECTION).toString()).isEqualTo("Disconnect[]");
        assertThat(new ClientSubscribed(CONNECTION, ClientRole.PLAYER, player).toString())
                .isEqualTo("ClientSubscribed[role=PLAYER, playerId=" + player + "]")
                .doesNotContain(CONNECTION);
    }
}
