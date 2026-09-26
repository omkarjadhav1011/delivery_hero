package app.deliveryhero.api;

import app.deliveryhero.common.DeliveryHeroException;
import app.deliveryhero.common.ProblemFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** The one place a {@link DeliveryHeroException} becomes a Problem Details response (document 13, section 6). */
@RestControllerAdvice
public class ProblemHandler {

    @ExceptionHandler(DeliveryHeroException.class)
    ResponseEntity<ProblemDetail> refused(DeliveryHeroException refusal) {
        ProblemDetail problem = ProblemFactory.of(refusal.code());
        String detail = refusal.detail();
        if (detail != null) {
            problem.setDetail(detail);
        }
        problem.setProperty("errors", refusal.errors());
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }
}
