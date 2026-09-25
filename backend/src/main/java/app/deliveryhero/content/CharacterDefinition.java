package app.deliveryhero.content;

import app.deliveryhero.common.Role;
import java.util.List;

/** A character as it is validated and saved. */
public record CharacterDefinition(
        Role role, String displayName, String introLine, List<String> correctLines, List<String> wrongLines) {}
