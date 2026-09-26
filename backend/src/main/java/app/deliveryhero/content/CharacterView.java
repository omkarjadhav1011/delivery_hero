package app.deliveryhero.content;

import app.deliveryhero.common.Role;
import java.util.List;

/** A character as the admin API returns it (API section 7.5). */
public record CharacterView(
        Role role,
        String displayName,
        String introLine,
        List<String> correctLines,
        List<String> wrongLines,
        int version) {}
