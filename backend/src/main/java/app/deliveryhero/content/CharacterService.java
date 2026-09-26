package app.deliveryhero.content;

import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.DeliveryHeroException;
import app.deliveryhero.common.Role;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Saves character changes for the admin panel, validated and version-checked (FR-073, FR-074). */
@Service
public class CharacterService {

    private static final TypeReference<List<String>> LINES = new TypeReference<>() {};

    private final CharacterRepository characters;
    private final ContentValidator validator;
    private final JsonMapper json;
    private final Clock clock;

    CharacterService(CharacterRepository characters, ContentValidator validator, JsonMapper json, Clock clock) {
        this.characters = characters;
        this.validator = validator;
        this.json = json;
        this.clock = clock;
    }

    // TODO(US-55): the character editor adds GET /api/admin/characters (S2-24)

    /** Saves the character when {@code version} is the stored one; otherwise EDIT_CONFLICT (FR-073). */
    @Transactional
    public CharacterView update(Role role, CharacterInput input) {
        CharacterEntity entity =
                characters.findById(role).orElseThrow(() -> new DeliveryHeroException(ApiErrorCode.NOT_FOUND));
        CharacterDefinition character = definition(role, input);
        Integer version = input.version();
        if (version == null || version != entity.version()) {
            throw new DeliveryHeroException(ApiErrorCode.EDIT_CONFLICT);
        }
        ValidationReport report = validator.validateCharacter(character);
        if (report.hasErrors()) {
            throw invalid(report.errors());
        }
        entity.apply(
                character,
                json.writeValueAsString(character.correctLines()),
                json.writeValueAsString(character.wrongLines()),
                clock.instant().truncatedTo(ChronoUnit.MICROS));
        try {
            return view(characters.saveAndFlush(entity));
        } catch (OptimisticLockingFailureException changedMeanwhile) {
            throw new DeliveryHeroException(ApiErrorCode.EDIT_CONFLICT);
        }
    }

    /** The input as a definition, or VALIDATION_FAILED listing each missing field. */
    private static CharacterDefinition definition(Role role, CharacterInput input) {
        List<Issue> missing = new ArrayList<>();
        require(missing, "displayName", input.displayName());
        require(missing, "introLine", input.introLine());
        List<String> correctLines = lines(missing, "correctLines", input.correctLines());
        List<String> wrongLines = lines(missing, "wrongLines", input.wrongLines());
        require(missing, "version", input.version());
        String displayName = input.displayName();
        String introLine = input.introLine();
        if (displayName == null
                || introLine == null
                || correctLines == null
                || wrongLines == null
                || !missing.isEmpty()) {
            throw invalid(missing);
        }
        return new CharacterDefinition(role, displayName, introLine, correctLines, wrongLines);
    }

    /** The lines, or null with a REQUIRED issue when the list or one of its lines is missing. */
    private static @Nullable List<String> lines(List<Issue> missing, String path, @Nullable List<String> lines) {
        if (lines == null || lines.contains(null)) {
            missing.add(new Issue(path, "REQUIRED", "This field is required."));
            return null;
        }
        return lines;
    }

    private CharacterView view(CharacterEntity entity) {
        return new CharacterView(
                entity.role(),
                entity.displayName(),
                entity.introLine(),
                json.readValue(entity.correctLines(), LINES),
                json.readValue(entity.wrongLines(), LINES),
                entity.version());
    }

    private static void require(List<Issue> missing, String path, @Nullable Object value) {
        if (value == null) {
            missing.add(new Issue(path, "REQUIRED", "This field is required."));
        }
    }

    private static DeliveryHeroException invalid(List<Issue> errors) {
        return new DeliveryHeroException(ApiErrorCode.VALIDATION_FAILED, null, errors);
    }
}
