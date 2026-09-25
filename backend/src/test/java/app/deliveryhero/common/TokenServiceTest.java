package app.deliveryhero.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Player tokens are random and only their hashes are kept (NFR-18, DEC-109). */
class TokenServiceTest {

    private final TokenService tokens = new TokenService(new SecureRandom());

    @Test
    @DisplayName("A new token is 22 URL-safe characters, and tokens don't repeat")
    void newTokensAreUrlSafeAndUnique() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            String token = tokens.newToken();
            assertThat(token).hasSize(22).matches("[A-Za-z0-9_-]+");
            assertThat(seen.add(token)).isTrue();
        }
    }

    @Test
    @DisplayName("The same token hashes the same, and the stored hash is never the raw token")
    void hashIsStableAndNotTheToken() {
        String token = tokens.newToken();

        String hash = tokens.hash(token);

        assertThat(tokens.hash(token)).isEqualTo(hash);
        assertThat(hash).isNotEqualTo(token).doesNotContain(token);
        assertThat(tokens.hash(tokens.newToken())).isNotEqualTo(hash);
    }

    @Test
    @DisplayName("Keys compare equal only when every character matches")
    void constantTimeComparison() {
        assertThat(TokenService.equalsConstantTime("K7PQ2M-key", "K7PQ2M-key")).isTrue();
        assertThat(TokenService.equalsConstantTime("K7PQ2M-key", "K7PQ2M-kez")).isFalse();
        assertThat(TokenService.equalsConstantTime("K7PQ2M-key", "K7PQ2M-key-longer"))
                .isFalse();
        assertThat(TokenService.equalsConstantTime("", "x")).isFalse();
    }
}
