package app.deliveryhero.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import app.deliveryhero.seed.SeedCommand;
import app.deliveryhero.support.IntegrationTest;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

/** The version-checked character update (API section 7.5) against the seeded characters (DS-01). */
@IntegrationTest
class CharacterApiIT {

    private static final Path DS_01 = Path.of("..", "seed", "delivery-hero-seed.json");
    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN");
    private static final List<String> CORRECT =
            List.of("Bug squashed!", "Test passed. I'm almost disappointed.", "Zero defects. Suspicious, but nice.");
    private static final List<String> WRONG =
            List.of("That bug just reached production.", "Reopening the ticket.", "Logged it. Severity: ouch.");

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private SeedCommand seed;

    @Autowired
    private JsonMapper json;

    @BeforeEach
    void seededCharacters() {
        jdbc.sql("DELETE FROM games").update();
        assertThat(seed.run(List.of(DS_01.toString()))).isEqualTo(SeedCommand.IMPORTED);
    }

    @Test
    @DisplayName("AC-US53-02 characters: two admins edit Tess from the same version; the second save is refused")
    void secondSaveOfTheSameVersionIsRefused() {
        int opened = testerVersion();

        MvcTestResult first = put("TESTER", character("Tessa", opened));
        MvcTestResult second = put("TESTER", character("Tess B", opened));

        assertThat(first).hasStatusOk();
        assertThat(first).bodyJson().isLenientlyEqualTo("""
                {"role": "TESTER", "displayName": "Tessa", "introLine": "Found another one!"}
                """);
        assertThat(first).bodyJson().extractingPath("$.version").isEqualTo(opened + 1);
        assertThat(second).hasStatus(HttpStatus.CONFLICT);
        assertThat(second).bodyJson().isLenientlyEqualTo("""
                {"status": 409, "code": "EDIT_CONFLICT",
                 "detail": "Someone else changed this since you opened it. Reload to see their changes."}
                """);
        assertThat(jdbc.sql("SELECT display_name FROM characters WHERE role = 'TESTER'")
                        .query(String.class)
                        .single())
                .isEqualTo("Tessa");
    }

    @Test
    @DisplayName("A character save breaking the limits is refused with VALIDATION_FAILED, and nothing changes")
    void invalidSaveIsRefused() {
        Map<String, Object> empty = character("", testerVersion());
        Map<String, Object> noVersion = character("Tessa", testerVersion());
        noVersion.remove("version");

        assertThat(put("TESTER", empty)).bodyJson().isLenientlyEqualTo("""
                {"status": 422, "code": "VALIDATION_FAILED", "errors": [{"path": "displayName"}]}
                """);
        assertThat(put("TESTER", noVersion)).bodyJson().isLenientlyEqualTo("""
                {"status": 422, "code": "VALIDATION_FAILED", "errors": [{"path": "version", "code": "REQUIRED"}]}
                """);
        assertThat(jdbc.sql("SELECT display_name FROM characters WHERE role = 'TESTER'")
                        .query(String.class)
                        .single())
                .isNotEqualTo("Tessa");
    }

    @Test
    @DisplayName("An unknown role answers 404 NOT_FOUND; without a session 401, and without the CSRF token 403")
    void unknownRoleNoSessionAndNoCsrf() {
        assertThat(put("WIZARD", character("Tessa", 0))).bodyJson().isLenientlyEqualTo("""
                {"status": 404, "code": "NOT_FOUND"}
                """);
        String body = json.writeValueAsString(character("Tessa", testerVersion()));
        assertThat(mvc.put()
                        .uri("/api/admin/characters/TESTER")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.put()
                        .uri("/api/admin/characters/TESTER")
                        .with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .exchange())
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    private int testerVersion() {
        return jdbc.sql("SELECT version FROM characters WHERE role = 'TESTER'")
                .query(Integer.class)
                .single();
    }

    private static Map<String, Object> character(String displayName, int version) {
        Map<String, Object> character = new java.util.HashMap<>();
        character.put("displayName", displayName);
        character.put("introLine", "Found another one!");
        character.put("correctLines", CORRECT);
        character.put("wrongLines", WRONG);
        character.put("version", version);
        return character;
    }

    private MvcTestResult put(String role, Map<String, Object> body) {
        return mvc.put()
                .uri("/api/admin/characters/{role}", role)
                .with(ADMIN)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))
                .exchange();
    }
}
