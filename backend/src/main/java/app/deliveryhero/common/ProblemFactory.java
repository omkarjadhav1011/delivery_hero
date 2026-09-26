package app.deliveryhero.common;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Builds the Problem Details body of every REST error: {@code type}, {@code title}, {@code status}, {@code code},
 * {@code detail} and {@code errors} (API section 6.1, LLD section 5.12).
 */
public final class ProblemFactory {

    private ProblemFactory() {}

    public static ProblemDetail of(ApiErrorCode code) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.valueOf(code.status()));
        problem.setTitle(code.title());
        problem.setDetail(code.detail());
        problem.setProperty("code", code.name());
        problem.setProperty("errors", List.of());
        return problem;
    }
}
