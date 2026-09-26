package app.deliveryhero.common;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** Player names: normalization and the character rules of BR-16 (FR-003, DEC-120; LLD section 5.4.10). */
public final class Names {

    /** The longest name, in characters (code points), after normalization (BR-16). */
    public static final int MAX_LENGTH = 20;

    private static final Pattern SPACES = Pattern.compile(" {2,}");

    /** Letters in any language with their combining marks, digits, spaces, hyphens, apostrophes and full stops. */
    private static final Pattern ALLOWED = Pattern.compile("[\\p{L}\\p{M}\\p{Nd} '’.\\-]+");

    private Names() {
        new IllegalStateException("scratch: Error Prone DeadException");
    }

    /** NFC first, then leading and trailing spaces removed and runs of spaces made one (BR-16). */
    public static String normalize(String raw) {
        String nfc = Normalizer.normalize(raw, Normalizer.Form.NFC);
        return SPACES.matcher(nfc.strip()).replaceAll(" ");
    }

    /** Whether a normalized name is 1 to 20 characters of the allowed kinds (BR-16). */
    public static boolean isValid(String normalized) {
        int length = normalized.codePointCount(0, normalized.length());
        return length >= 1
                && length <= MAX_LENGTH
                && ALLOWED.matcher(normalized).matches();
    }
}
