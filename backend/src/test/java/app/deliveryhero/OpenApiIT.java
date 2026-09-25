package app.deliveryhero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import app.deliveryhero.support.IntegrationTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The committed {@code docs/openapi.json} matches the document generated from the code (NFR-44, DEC-175). When they
 * differ, the generated version is written to {@code target/openapi.json} for review (SG-05).
 */
@IntegrationTest
class OpenApiIT {

    private static final Path COMMITTED = Path.of("..", "docs", "openapi.json");
    private static final Path GENERATED = Path.of("target", "openapi.json");
    private static final JsonMapper JSON =
            JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    @Autowired
    private MockMvcTester mvc;

    @Test
    @DisplayName("docs/openapi.json matches the OpenAPI document generated from the code")
    void committedDocumentMatchesTheCode() throws IOException {
        MvcTestResult result = mvc.get().uri("/v3/api-docs").exchange();
        assertThat(result).hasStatusOk();
        JsonNode generated = JSON.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        if (Files.exists(COMMITTED) && JSON.readTree(COMMITTED.toFile()).equals(generated)) {
            return;
        }
        String pretty = JSON.writeValueAsString(generated).replace("\r\n", "\n") + "\n";
        Files.createDirectories(GENERATED.getParent());
        Files.writeString(GENERATED, pretty, StandardCharsets.UTF_8);
        fail("docs/openapi.json differs from the generated document. Review backend/target/openapi.json, update"
                + " document 11 if the API changed, and copy it to docs/openapi.json (Setup Guide section 8.5)");
    }
}
