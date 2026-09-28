package app.deliveryhero.common;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** A refused request, carried to the one place that turns it into Problem Details (document 13, section 6; DEC-144). */
public class DeliveryHeroException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ApiErrorCode code;
    private final @Nullable String detail;

    /** The issues listed in the body's {@code errors}, each serialized as it is (API section 6.3). */
    private final transient List<?> errors;

    /** Further members of the body, such as {@code currentState} for NOT_ALLOWED_NOW (API section 6.2). */
    private final transient Map<String, Object> properties;

    public DeliveryHeroException(ApiErrorCode code) {
        this(code, null, List.of());
    }

    /** A refusal whose body lists issues and, when not null, words its own detail instead of the code's. */
    public DeliveryHeroException(ApiErrorCode code, @Nullable String detail, List<?> errors) {
        this(code, detail, errors, Map.of());
    }

    private DeliveryHeroException(
            ApiErrorCode code, @Nullable String detail, List<?> errors, Map<String, Object> properties) {
        super(code.name());
        this.code = code;
        this.detail = detail;
        this.errors = List.copyOf(errors);
        this.properties = Map.copyOf(properties);
    }

    /** A host action that no longer applies, with the state the panel refreshes to (FR-081). */
    public static DeliveryHeroException notAllowedNow(GameState currentState) {
        return new DeliveryHeroException(
                ApiErrorCode.NOT_ALLOWED_NOW, null, List.of(), Map.of("currentState", currentState.name()));
    }

    public ApiErrorCode code() {
        return code;
    }

    public @Nullable String detail() {
        return detail;
    }

    public List<?> errors() {
        return errors;
    }

    public Map<String, Object> properties() {
        return properties;
    }
}
