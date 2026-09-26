package app.deliveryhero.common;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** A refused request, carried to the one place that turns it into Problem Details (document 13, section 6; DEC-144). */
public class DeliveryHeroException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ApiErrorCode code;
    private final @Nullable String detail;

    /** The issues listed in the body's {@code errors}, each serialized as it is (API section 6.3). */
    private final transient List<?> errors;

    public DeliveryHeroException(ApiErrorCode code) {
        this(code, null, List.of());
    }

    /** A refusal whose body lists issues and, when not null, words its own detail instead of the code's. */
    public DeliveryHeroException(ApiErrorCode code, @Nullable String detail, List<?> errors) {
        super(code.name());
        this.code = code;
        this.detail = detail;
        this.errors = List.copyOf(errors);
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
}
