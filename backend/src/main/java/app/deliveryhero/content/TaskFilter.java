package app.deliveryhero.content;

import app.deliveryhero.common.Phase;
import app.deliveryhero.common.Role;
import app.deliveryhero.common.TaskKind;
import app.deliveryhero.common.TaskType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

/** The task library's filters; each one left out matches every task, and {@code q} searches prompts (FR-070). */
public record TaskFilter(
        @Nullable Role role,
        @Nullable Phase phase,
        @Nullable TaskKind kind,
        @Nullable TaskType type,
        @Nullable String q) {

    private static final char ESCAPE = '\\';

    /** The filters as one query, so PostgreSQL does the filtering (document 10, section 12). */
    Specification<TaskEntity> specification() {
        return (task, query, where) -> {
            List<Predicate> all = new ArrayList<>();
            if (role != null) {
                all.add(where.equal(task.get("role"), role));
            }
            if (phase != null) {
                all.add(where.equal(task.get("phase"), phase));
            }
            if (kind != null) {
                all.add(where.equal(task.get("kind"), kind));
            }
            if (type != null) {
                all.add(where.equal(task.get("taskType"), type));
            }
            String search = q == null ? "" : q.strip();
            if (!search.isEmpty()) {
                all.add(where.like(where.lower(task.get("prompt")), "%" + literal(search) + "%", ESCAPE));
            }
            return where.and(all.toArray(Predicate[]::new));
        };
    }

    /** The search text in lower case with LIKE's wildcards escaped, so they match themselves. */
    private static String literal(String search) {
        StringBuilder escaped = new StringBuilder();
        for (char c : search.toLowerCase(Locale.ROOT).toCharArray()) {
            if (c == '%' || c == '_' || c == ESCAPE) {
                escaped.append(ESCAPE);
            }
            escaped.append(c);
        }
        return escaped.toString();
    }
}
