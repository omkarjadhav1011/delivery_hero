package app.deliveryhero.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** One row per destination and role, as API section 8.2 and HLD section 11 list them. */
class DestinationPolicyTest {

    private static final UUID GAME = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID OTHER_GAME = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private static final ClientPrincipal PLAYER =
            new PlayerPrincipal(GAME, UUID.fromString("00000000-0000-0000-0000-0000000000b1"));
    private static final ClientPrincipal PROJECTOR = new ProjectorPrincipal(GAME);
    private static final ClientPrincipal ADMIN = new AdminPrincipal();

    private final DestinationPolicy policy = new DestinationPolicy();

    static Stream<Arguments> subscriptions() {
        String screen = "/topic/games/" + GAME + "/screen";
        String otherScreen = "/topic/games/" + OTHER_GAME + "/screen";
        String admin = "/topic/games/" + GAME + "/admin";
        return Stream.of(
                Arguments.of("player", PLAYER, "/user/queue/game", true),
                Arguments.of("player", PLAYER, "/user/queue/time-sync", true),
                Arguments.of("player", PLAYER, screen, false),
                Arguments.of("player", PLAYER, admin, false),
                Arguments.of("player", PLAYER, "/topic/games", false),
                Arguments.of("projector", PROJECTOR, screen, true),
                Arguments.of("projector", PROJECTOR, otherScreen, false),
                Arguments.of("projector", PROJECTOR, "/user/queue/time-sync", true),
                Arguments.of("projector", PROJECTOR, "/user/queue/game", false),
                Arguments.of("projector", PROJECTOR, admin, false),
                Arguments.of("admin", ADMIN, admin, true),
                Arguments.of("admin", ADMIN, "/user/queue/time-sync", true),
                Arguments.of("admin", ADMIN, "/user/queue/game", false),
                Arguments.of("admin", ADMIN, screen, false),
                Arguments.of("player", PLAYER, "/queue/game", false),
                Arguments.of("player", PLAYER, "/user/queue/game/extra", false),
                Arguments.of("projector", PROJECTOR, "/topic/games/" + GAME + "/screen/../admin", false),
                Arguments.of("player", PLAYER, null, false));
    }

    static Stream<Arguments> sends() {
        String answer = "/app/games/" + GAME + "/answer";
        String otherAnswer = "/app/games/" + OTHER_GAME + "/answer";
        return Stream.of(
                Arguments.of("player", PLAYER, answer, true),
                Arguments.of("player", PLAYER, otherAnswer, false),
                Arguments.of("player", PLAYER, "/app/time-sync", true),
                Arguments.of("projector", PROJECTOR, "/app/time-sync", true),
                Arguments.of("projector", PROJECTOR, answer, false),
                Arguments.of("admin", ADMIN, "/app/time-sync", true),
                Arguments.of("admin", ADMIN, answer, false),
                Arguments.of("player", PLAYER, "/topic/games/" + GAME + "/screen", false),
                Arguments.of("player", PLAYER, "/user/queue/game", false),
                Arguments.of("player", PLAYER, null, false));
    }

    @ParameterizedTest(name = "AC-EN04-02 {0} subscribing to {2}: allowed {3}")
    @MethodSource("subscriptions")
    @DisplayName("AC-EN04-02 subscriptions follow API section 8.2")
    void subscribe(String role, ClientPrincipal principal, String destination, boolean allowed) {
        assertThat(policy.maySubscribe(principal, destination)).isEqualTo(allowed);
    }

    @ParameterizedTest(name = "AC-EN04-02 {0} sending to {2}: allowed {3}")
    @MethodSource("sends")
    @DisplayName("AC-EN04-02 sends follow API section 8.2")
    void send(String role, ClientPrincipal principal, String destination, boolean allowed) {
        assertThat(policy.maySend(principal, destination)).isEqualTo(allowed);
    }
}
