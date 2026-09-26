package app.deliveryhero.api.admin;

import app.deliveryhero.api.ProblemHandler;
import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.DeliveryHeroException;
import app.deliveryhero.content.Issue;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Requests Spring can't bind, answered with the admin API's codes rather than a bare 400, so every admin error has a
 * {@code code} (API section 6.1, DEC-144): an unknown ID is NOT_FOUND, anything else VALIDATION_FAILED.
 */
@RestControllerAdvice(basePackageClasses = AdminRequestProblems.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class AdminRequestProblems {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> unreadable(HttpMessageNotReadableException unreadable) {
        return invalid(new Issue("body", "PATTERN", "The request body isn't valid JSON of the expected shape."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ProblemDetail> missing(MissingServletRequestParameterException missing) {
        return invalid(new Issue(missing.getParameterName(), "REQUIRED", "This field is required."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> mismatch(MethodArgumentTypeMismatchException mismatch) {
        // A path segment names an item (a task ID, a character's role), so one that can't exist is NOT_FOUND
        if (mismatch.getParameter().hasParameterAnnotation(PathVariable.class)) {
            return ProblemHandler.response(new DeliveryHeroException(ApiErrorCode.NOT_FOUND));
        }
        return invalid(new Issue(mismatch.getName(), "PATTERN", "This value has the wrong format."));
    }

    private static ResponseEntity<ProblemDetail> invalid(Issue issue) {
        return ProblemHandler.response(new DeliveryHeroException(ApiErrorCode.VALIDATION_FAILED, null, List.of(issue)));
    }
}
