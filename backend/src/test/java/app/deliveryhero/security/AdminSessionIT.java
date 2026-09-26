package app.deliveryhero.security;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.support.PostgresTestConfiguration;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * The admin session cookie as Tomcat really sends it, which MockMvc can't show (LLD section 5.9, DEC-97): its name and
 * flags, and logout ending the session behind it.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class AdminSessionIT {

    /** The public local-only password whose hash is in application-test.yml (SG-02). */
    private static final String LOCAL_PASSWORD = "delivery-hero-local";

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("AC-US49-04 cookie flags: DH_SESSION is HttpOnly, Secure and SameSite=Strict")
    void sessionCookieFlags() throws Exception {
        String setCookie = login().headers().allValues("Set-Cookie").stream()
                .filter(header -> header.startsWith("DH_SESSION="))
                .findFirst()
                .orElseThrow();

        List<String> attributes = List.of(setCookie.toLowerCase().split(";\\s*"));
        assertThat(attributes).contains("httponly", "secure", "samesite=strict");
    }

    @Test
    @DisplayName("AC-US49-03 logout: the old session cookie is refused afterwards")
    void logoutEndsTheSession() throws Exception {
        String session = cookie(login(), "DH_SESSION").orElseThrow();
        assertThat(get("/api/admin/session", session).body()).contains("\"authenticated\":true");

        // Login rotates the CSRF token, so the session check hands out the new one
        String csrf = cookie(get("/api/admin/session", session), "XSRF-TOKEN").orElseThrow();
        // Without the token, logout is refused and the session lives on (API 5.2, DI-18)
        HttpResponse<String> forged = send(HttpRequest.newBuilder(uri("/api/admin/logout"))
                .header("Cookie", session)
                .POST(HttpRequest.BodyPublishers.noBody()));
        assertThat(forged.statusCode()).isEqualTo(403);
        assertThat(get("/api/admin/session", session).body()).contains("\"authenticated\":true");

        HttpResponse<String> logout = send(HttpRequest.newBuilder(uri("/api/admin/logout"))
                .header("Cookie", session + "; " + csrf)
                .header("X-XSRF-TOKEN", value(csrf))
                .POST(HttpRequest.BodyPublishers.noBody()));
        assertThat(logout.statusCode()).isEqualTo(204);

        assertThat(get("/api/admin/session", session).body()).contains("\"authenticated\":false");
        assertThat(get("/api/admin/tasks", session).statusCode()).isEqualTo(401);
    }

    /** Logs in as the single-page app does: load the CSRF cookie, then post the form with it (API section 7.3). */
    private HttpResponse<String> login() throws IOException, InterruptedException {
        String csrf = cookie(get("/api/admin/session", null), "XSRF-TOKEN").orElseThrow();
        HttpResponse<String> login = send(HttpRequest.newBuilder(uri("/api/admin/login"))
                .header("Cookie", csrf)
                .header("X-XSRF-TOKEN", value(csrf))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("username=admin&password=" + LOCAL_PASSWORD)));
        assertThat(login.statusCode()).isEqualTo(204);
        return login;
    }

    private HttpResponse<String> get(String path, @Nullable String cookie) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return send(request);
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws IOException, InterruptedException {
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    /** The {@code name=value} pair of a cookie the response sets. */
    private static Optional<String> cookie(HttpResponse<?> response, String name) {
        return response.headers().allValues("Set-Cookie").stream()
                .filter(header -> header.startsWith(name + "="))
                .map(header -> header.split(";", 2)[0])
                .findFirst();
    }

    private static String value(String cookie) {
        return cookie.substring(cookie.indexOf('=') + 1);
    }
}
