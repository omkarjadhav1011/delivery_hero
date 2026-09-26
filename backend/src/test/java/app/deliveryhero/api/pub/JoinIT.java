package app.deliveryhero.api.pub;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.common.GameState;
import app.deliveryhero.common.TokenService;
import app.deliveryhero.engine.GameEngine;
import app.deliveryhero.realtime.CredentialRegistry;
import app.deliveryhero.support.IntegrationTest;
import app.deliveryhero.support.TestData;
import java.io.UnsupportedEncodingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

/** Joining a game by its code over REST (API section 7.2, LLD section 5.4.10). */
@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
class JoinIT {

    private static final String INACTIVE = "This game link isn't active. Ask the host for the current link.";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private GameEngine engine;

    @Autowired
    private CredentialRegistry credentials;

    @Autowired
    private TokenService tokens;

    @AfterEach
    void discardGame() {
        engine.discard(TestData.GAME_ID);
    }

    @Test
    @DisplayName("AC-US01-03 inactive link: a code with no game gives 404 GAME_NOT_ACTIVE for status and for joining")
    void unknownCodeIsNotActive() {
        assertNotActive(
                mvc.get().uri("/api/games/{code}", TestData.UNKNOWN_CODE).exchange());
        assertNotActive(join(TestData.UNKNOWN_CODE, "Priya"));
    }

    @Test
    @DisplayName("AC-US01-03 inactive link: a discarded game's code gives 404 GAME_NOT_ACTIVE")
    void discardedGameIsNotActive() {
        openLobby();
        engine.discard(TestData.GAME_ID);

        assertNotActive(mvc.get().uri("/api/games/{code}", TestData.GAME_CODE).exchange());
        assertNotActive(join(TestData.GAME_CODE, "Priya"));
    }

    @Test
    @DisplayName("The status of a game in Lobby says it's joinable")
    void lobbyIsJoinable() {
        openLobby();

        MvcTestResult status =
                mvc.get().uri("/api/games/{code}", TestData.GAME_CODE).exchange();

        assertThat(status).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {"gameId": "%s", "code": "K7PQ2M", "state": "LOBBY", "test": false, "joinable": true, "reason": null}
                """.formatted(TestData.GAME_ID));
    }

    @Test
    @DisplayName(
            "AC-US02-01 Priya joins K7PQ2M typing \"  Priya   S \": 201 with \"Priya S\" and a 22-character token that"
                    + " connects")
    void priyaJoins(CapturedOutput output) {
        openLobby();

        MvcTestResult joined = join(TestData.GAME_CODE, TestData.PRIYA_TYPED);

        assertThat(joined).hasStatus(HttpStatus.CREATED);
        assertThat(joined).bodyJson().extractingPath("$.name").isEqualTo(TestData.PRIYA);
        assertThat(joined).bodyJson().extractingPath("$.gameId").isEqualTo(TestData.GAME_ID.toString());
        String token = field(joined, "token");
        String playerId = field(joined, "playerId");
        assertThat(token).hasSize(22).matches("[A-Za-z0-9_-]+");
        assertThat(credentials.playerByTokenHash(tokens.hash(token)))
                .hasValueSatisfying(
                        player -> assertThat(player.playerId().toString()).isEqualTo(playerId));
        assertThat(output.getOut())
                .contains("PLAYER_JOINED")
                .contains(playerId)
                .doesNotContain("Priya")
                .doesNotContain(token);
    }

    @Test
    @DisplayName("AC-US02-03 a second \"priya s\" joins as \"priya s 2\"")
    void duplicateNamesGetANumber() {
        openLobby();
        join(TestData.GAME_CODE, TestData.PRIYA_TYPED);

        MvcTestResult second = join(TestData.GAME_CODE, "priya s");

        assertThat(second).hasStatus(HttpStatus.CREATED);
        assertThat(second).bodyJson().extractingPath("$.name").isEqualTo("priya s 2");
    }

    @Test
    @DisplayName("AC-US02-02 a name containing \"@\" gives 422 INVALID_NAME with the naming-rules message")
    void invalidNameIsRefused() {
        openLobby();

        MvcTestResult refused = join(TestData.GAME_CODE, "priya@home");

        assertThat(refused)
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson()
                .isLenientlyEqualTo("""
                {"status": 422, "code": "INVALID_NAME", "errors": [],
                 "detail": "Names can use letters, numbers, spaces, hyphens, apostrophes and full stops, up to 20 characters."}
                """);
    }

    @Test
    @DisplayName("The end-to-end game endpoint doesn't exist outside the e2e profile")
    void e2eGameEndpointIsAbsent() {
        assertThat(mvc.post().uri("/api/test/s0-game").exchange()).hasStatus4xxClientError();
    }

    private void openLobby() {
        engine.create(TestData.GAME_ID, TestData.GAME_CODE, GameState.LOBBY, false);
    }

    private MvcTestResult join(String code, String name) {
        return mvc.post()
                .uri("/api/games/{code}/players", code)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"" + name + "\"}")
                .exchange();
    }

    private static void assertNotActive(MvcTestResult result) {
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND).bodyJson().isLenientlyEqualTo("""
                {"status": 404, "code": "GAME_NOT_ACTIVE", "detail": "%s", "errors": []}
                """.formatted(INACTIVE));
    }

    private static String field(MvcTestResult result, String name) {
        try {
            return JsonMapper.shared()
                    .readTree(result.getResponse().getContentAsString())
                    .get(name)
                    .asString();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
