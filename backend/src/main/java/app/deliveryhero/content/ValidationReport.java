package app.deliveryhero.content;

import java.util.List;

/** The result of {@link ContentValidator}: errors refuse the content, warnings don't (LLD section 5.3). */
public record ValidationReport(List<Issue> errors, List<Issue> warnings) {

    public ValidationReport {
        errors = List.copyOf(errors);
        warnings = List.copyOf(warnings);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
