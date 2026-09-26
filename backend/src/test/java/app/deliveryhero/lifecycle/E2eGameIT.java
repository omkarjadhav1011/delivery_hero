package app.deliveryhero.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.engine.GameEngine;
import app.deliveryhero.support.PostgresTestConfiguration;
import app.deliveryhero.support.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** The {@code e2e} profile's game for the walking-skeleton specs (DI-08, DI-24); test scaffolding until S1-04. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "e2e"})
@Import(PostgresTestConfiguration.class)
class E2eGameIT {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private GameEngine engine;

    @Autowired
    private JdbcClient jdbc;

    @AfterEach
    void discardGame() {
        engine.findByCode(TestData.GAME_CODE).ifPresent(game -> engine.discard(game.id()));
    }

    @Test
    @DisplayName("Opening the S0 game gives a fresh LOBBY game K7PQ2M each time, in memory only, so names start over")
    void opensAFreshLobbyGame() {
        MvcTestResult first = open();
        assertThat(first).hasStatus(HttpStatus.CREATED);
        assertThat(join("Priya")).bodyJson().extractingPath("$.name").isEqualTo("Priya");

        MvcTestResult second = open();

        assertThat(second).hasStatus(HttpStatus.CREATED);
        assertThat(mvc.get().uri("/api/games/{code}", TestData.GAME_CODE).exchange())
                .bodyJson()
                .extractingPath("$.state")
                .isEqualTo("LOBBY");
        assertThat(join("Priya")).bodyJson().extractingPath("$.name").isEqualTo("Priya");
        assertThat(jdbc.sql("SELECT count(*) FROM games").query(Long.class).single())
                .as("no games row, so the deploy lock and the seed command are unaffected")
                .isZero();
    }

    private MvcTestResult open() {
        return mvc.post().uri(E2eGameController.PATH).exchange();
    }

    private MvcTestResult join(String name) {
        return mvc.post()
                .uri("/api/games/{code}/players", TestData.GAME_CODE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"" + name + "\"}")
                .exchange();
    }
}
