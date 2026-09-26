package app.deliveryhero.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** IDs come from the injected generator (LLD section 4). */
class IdsTest {

    private static SecureRandom seeded() throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed(7L);
        return random;
    }

    @Test
    @DisplayName("A new ID is a version 4 UUID, and the same seed gives the same IDs")
    void versionFourAndRepeatable() throws NoSuchAlgorithmException {
        UUID first = Ids.newUuid(seeded());

        assertThat(first.version()).isEqualTo(4);
        assertThat(first.variant()).isEqualTo(2);
        assertThat(Ids.newUuid(seeded())).isEqualTo(first);
    }
}
