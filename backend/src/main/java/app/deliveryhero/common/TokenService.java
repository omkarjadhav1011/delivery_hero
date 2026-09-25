package app.deliveryhero.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Creates player tokens and hashes them: a token is 16 random bytes as URL-safe Base64 without padding, and only its
 * SHA-256 hash is ever kept (NFR-18, DEC-109, LLD section 5.4.10).
 */
@Component
public class TokenService {

    private static final int TOKEN_BYTES = 16;
    private static final Base64.Encoder URL_SAFE = Base64.getUrlEncoder().withoutPadding();

    private final SecureRandom random;

    public TokenService(SecureRandom random) {
        this.random = random;
    }

    public String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return URL_SAFE.encodeToString(bytes);
    }

    /** The SHA-256 hash of a token, as URL-safe Base64: the only form that is stored or looked up. */
    public String hash(String token) {
        return URL_SAFE.encodeToString(sha256().digest(token.getBytes(StandardCharsets.UTF_8)));
    }

    /** Compares two secrets in time that doesn't depend on where they differ (NFR-18). */
    public static boolean equalsConstantTime(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every Java runtime provides SHA-256", e);
        }
    }
}
