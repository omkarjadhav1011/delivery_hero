package app.deliveryhero.engine;

import app.deliveryhero.common.Names;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The names taken in one game, compared ignoring case (FR-004, BR-16). Used only on the game's session thread.
 */
public final class NameRegistry {

    private final Set<String> taken = new HashSet<>();

    /**
     * Takes a normalized, valid name, or the same name with the lowest free number from 2 upward, shortening the base
     * so the result fits 20 characters (BR-16). What the player typed keeps its case.
     */
    public String unique(String base) {
        String name = base;
        for (int number = 2; taken.contains(key(name)); number++) {
            String suffix = " " + number;
            name = shorten(base, Names.MAX_LENGTH - suffix.length()) + suffix;
        }
        taken.add(key(name));
        return name;
    }

    /** Frees a name, when the player is renamed or removed. */
    public void release(String name) {
        taken.remove(key(name));
    }

    private static String shorten(String base, int maxCodePoints) {
        if (base.codePointCount(0, base.length()) <= maxCodePoints) {
            return base;
        }
        return base.substring(0, base.offsetByCodePoints(0, maxCodePoints)).stripTrailing();
    }

    private static String key(String name) {
        return name.toUpperCase(Locale.ROOT).toLowerCase(Locale.ROOT);
    }
}
