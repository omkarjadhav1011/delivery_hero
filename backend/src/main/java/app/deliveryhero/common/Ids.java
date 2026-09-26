package app.deliveryhero.common;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Random version 4 UUIDs from the injected generator, so only config creates generators and tests can seed them (LLD
 * section 4; document 13, section 6).
 */
public final class Ids {

    private Ids() {}

    public static UUID newUuid(SecureRandom random) {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        bytes[6] = (byte) ((bytes[6] & 0x0f) | 0x40); // version 4
        bytes[8] = (byte) ((bytes[8] & 0x3f) | 0x80); // IETF variant
        long high = 0;
        long low = 0;
        for (int i = 0; i < 8; i++) {
            high = (high << 8) | (bytes[i] & 0xff);
            low = (low << 8) | (bytes[i + 8] & 0xff);
        }
        return new UUID(high, low);
    }
}
