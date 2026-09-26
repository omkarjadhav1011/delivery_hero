package app.deliveryhero.common;

/** A refused request, carried to the one place that turns it into Problem Details (document 13, section 6; DEC-144). */
public class DeliveryHeroException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ApiErrorCode code;

    public DeliveryHeroException(ApiErrorCode code) {
        super(code.name());
        this.code = code;
    }

    public ApiErrorCode code() {
        return code;
    }
}
