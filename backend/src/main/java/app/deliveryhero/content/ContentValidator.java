package app.deliveryhero.content;

import app.deliveryhero.common.Phase;
import app.deliveryhero.common.TaskKind;
import app.deliveryhero.common.TaskType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Every field rule of SRS section 7.3 for characters, tasks and run plans, in one place (LLD section 5.3), with the
 * issue codes of document 11, section 6.3.
 */
@Component
public class ContentValidator {

    private static final Pattern KEY = Pattern.compile("^[a-z0-9-]{1,40}$");
    private static final Pattern MARKED_WORD = Pattern.compile("^\\{\\{[^{}\\s]+}}$");
    private static final Set<String> LANGUAGES =
            Set.of("text", "java", "javascript", "typescript", "sql", "json", "python", "shell");
    private static final int PROMPT_WORDS_WARNING = 25;
    private static final int CODE_LINES_WARNING = 12;

    public ValidationReport validateCharacter(CharacterDefinition character) {
        Issues issues = new Issues();
        issues.length("displayName", character.displayName(), 1, 20);
        issues.length("introLine", character.introLine(), 1, 80);
        issues.reactionLines("correctLines", character.correctLines());
        issues.reactionLines("wrongLines", character.wrongLines());
        return issues.report();
    }

    /** The task rules for an admin save; a missing explanation is only a warning here (SRS 7.3). */
    public ValidationReport validateTask(TaskDefinition task) {
        Issues issues = new Issues();
        checkTask(task, issues);
        if (task.kind() != TaskKind.PRACTICE && isBlank(task.explanation())) {
            issues.warning(
                    "explanation", "MISSING_EXPLANATION", "Scored and incident tasks should have an explanation.");
        }
        return issues.report();
    }

    /** The task rules, plus the explanation the seed file requires for scored and incident tasks (SRS 7.3). */
    public ValidationReport validateSeedTask(TaskDefinition task) {
        Issues issues = new Issues();
        checkTask(task, issues);
        if (task.kind() != TaskKind.PRACTICE && isBlank(task.explanation())) {
            issues.error("explanation", "REQUIRED", "Scored and incident tasks need an explanation.");
        }
        return issues.report();
    }

    public ValidationReport validateRunPlan(RunPlanDefinition plan) {
        Issues issues = new Issues();
        issues.key("key", plan.key());
        issues.length("name", plan.name(), 1, 60);
        if (plan.roundLengthMinutes() < 3 || plan.roundLengthMinutes() > 10) {
            issues.error("roundLengthMinutes", "OUT_OF_RANGE", "The round length must be 3 to 10 whole minutes.");
        }
        return issues.report();
    }

    /**
     * The keys a run plan's lists name: each must name an existing task (SRS section 7.4), and a task appears once per
     * plan, because that is the key of {@code run_plan_entries} (document 10, section 7.4). Which list a task may be
     * in, and empty phases, are readiness errors (BR-13), checked when a game is created.
     */
    public ValidationReport validateRunPlanKeys(RunPlanDefinition plan, Predicate<String> taskExists) {
        Issues issues = new Issues();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < plan.practice().size(); i++) {
            issues.entry("practice[" + i + "]", plan.practice().get(i), taskExists, seen);
        }
        String incident = plan.incident();
        if (incident != null) {
            issues.entry("incident", incident, taskExists, seen);
        }
        for (Phase phase : Phase.values()) {
            List<String> keys = plan.phases().getOrDefault(phase, List.of());
            for (int i = 0; i < keys.size(); i++) {
                issues.entry("phases." + phase + "[" + i + "]", keys.get(i), taskExists, seen);
            }
        }
        return issues.report();
    }

    private static void checkTask(TaskDefinition task, Issues issues) {
        issues.key("key", task.key());
        if (task.kind() == TaskKind.SCORED && task.phase() == null) {
            issues.error("phase", "PHASE_REQUIRED", "Scored tasks need a phase.");
        } else if (task.kind() != TaskKind.SCORED && task.phase() != null) {
            issues.error("phase", "PHASE_NOT_ALLOWED", "Practice and incident tasks have no phase.");
        }
        if (task.kind() == TaskKind.INCIDENT && task.type() != TaskType.MULTIPLE_CHOICE) {
            issues.error("type", "INCIDENT_NOT_MULTIPLE_CHOICE", "Incident tasks must be multiple choice.");
        }
        if (issues.length("prompt", task.prompt(), 1, 200) && words(task.prompt()) > PROMPT_WORDS_WARNING) {
            issues.warning("prompt", "PROMPT_OVER_25_WORDS", "The prompt has more than 25 words.");
        }
        CodeSnippet code = task.code();
        if (code != null) {
            checkCode(code, issues);
        }
        Integer limit = task.timeLimitSeconds();
        if (limit != null && (limit < 5 || limit > 60)) {
            issues.error("timeLimitSeconds", "OUT_OF_RANGE", "The time limit must be 5 to 60 seconds.");
        }
        String explanation = task.explanation();
        if (explanation != null && explanation.length() > 300) {
            issues.error("explanation", "TOO_LONG", "The explanation must be at most 300 characters.");
        }
        switch (task.content()) {
            case MultipleChoiceContent c -> checkMultipleChoice(c, issues);
            case YesNoContent c -> {}
            case OrderContent c -> checkOrder(c, issues);
            case ProblemWordsContent c -> checkProblemWords(c, issues);
        }
    }

    private static void checkCode(CodeSnippet code, Issues issues) {
        if (!LANGUAGES.contains(code.language())) {
            issues.error("code.language", "PATTERN", "The code language must be one of " + LANGUAGES + ".");
        }
        long lines = code.text().chars().filter(ch -> ch == '\n').count() + 1;
        if (code.text().length() > 2000 || lines > 30) {
            issues.error("code.text", "TOO_LONG", "The code must be at most 2,000 characters and 30 lines.");
        } else if (lines > CODE_LINES_WARNING) {
            issues.warning("code.text", "CODE_OVER_12_LINES", "The code has more than 12 lines.");
        }
    }

    private static void checkMultipleChoice(MultipleChoiceContent content, Issues issues) {
        List<MultipleChoiceContent.Option> options = content.options();
        if (options.size() < 2 || options.size() > 4) {
            issues.error("content.options", "OUT_OF_RANGE", "Multiple choice needs 2 to 4 options.");
        }
        for (int i = 0; i < options.size(); i++) {
            issues.length("content.options[" + i + "].text", options.get(i).text(), 1, 80);
        }
        if (options.stream().filter(MultipleChoiceContent.Option::correct).count() != 1) {
            issues.error("content.options", "EXACTLY_ONE_CORRECT", "Choose exactly one correct option.");
        }
    }

    private static void checkOrder(OrderContent content, Issues issues) {
        List<OrderContent.Item> items = content.items();
        for (int i = 0; i < items.size(); i++) {
            issues.length("content.items[" + i + "].text", items.get(i).text(), 1, 60);
        }
        if (items.size() < 3 || items.size() > 5) {
            issues.error("content.items", "OUT_OF_RANGE", "Tap to order needs 3 to 5 items.");
            return;
        }
        List<Integer> positions =
                items.stream().map(OrderContent.Item::correctPosition).toList();
        List<Integer> expected =
                java.util.stream.IntStream.rangeClosed(1, items.size()).boxed().toList();
        if (!positions.stream().sorted().toList().equals(expected)) {
            issues.error(
                    "content.items",
                    "POSITIONS_INVALID",
                    "Each item needs a different correct position from 1 to " + items.size() + ".");
        } else if (positions.equals(expected)) {
            issues.error(
                    "content.items", "ORDER_SAME_AS_CORRECT", "The display order must differ from the correct order.");
        }
    }

    private static void checkProblemWords(ProblemWordsContent content, Issues issues) {
        if (!issues.length("content.markedText", content.markedText(), 1, 200)) {
            return;
        }
        List<String> marked = List.of(content.markedText().trim().split("\\s+")).stream()
                .filter(token -> token.contains("{{") || token.contains("}}"))
                .toList();
        if (marked.stream().anyMatch(token -> !MARKED_WORD.matcher(token).matches())) {
            issues.error(
                    "content.markedText", "MARKER_NOT_WHOLE_WORD", "Each {{marker}} must wrap exactly one whole word.");
        } else if (marked.isEmpty() || marked.size() > 4) {
            issues.error("content.markedText", "MARKED_WORDS_COUNT", "Mark 1 to 4 problem words.");
        }
    }

    private static int words(String text) {
        String trimmed = text.trim();
        return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
    }

    private static boolean isBlank(@Nullable String text) {
        return text == null || text.isBlank();
    }

    /** Collects errors and warnings in the order the rules run. */
    private static final class Issues {

        private final List<Issue> errors = new ArrayList<>();
        private final List<Issue> warnings = new ArrayList<>();

        void error(String path, String code, String message) {
            errors.add(new Issue(path, code, message));
        }

        void warning(String path, String code, String message) {
            warnings.add(new Issue(path, code, message));
        }

        void key(String path, String key) {
            if (!KEY.matcher(key).matches()) {
                error(path, "PATTERN", "Keys are 1 to 40 lowercase letters, digits and hyphens.");
            }
        }

        boolean length(String path, String text, int min, int max) {
            if (text.length() < min) {
                error(path, min == 1 ? "REQUIRED" : "TOO_SHORT", "Must be " + min + " to " + max + " characters.");
                return false;
            }
            if (text.length() > max) {
                error(path, "TOO_LONG", "Must be " + min + " to " + max + " characters.");
                return false;
            }
            return true;
        }

        void reactionLines(String path, List<String> lines) {
            if (lines.size() != 3) {
                error(path, "OUT_OF_RANGE", "Exactly 3 lines are needed.");
                return;
            }
            for (int i = 0; i < lines.size(); i++) {
                length(path + "[" + i + "]", lines.get(i), 1, 80);
            }
        }

        // TODO(US-56): document 11, section 6.3 has no code for a key that names no task (DI-35)
        void entry(String path, String key, Predicate<String> taskExists, Set<String> seen) {
            if (!taskExists.test(key)) {
                error(path, "UNKNOWN_KEY", "No task has the key " + key + ".");
            } else if (!seen.add(key)) {
                error(path, "DUPLICATE_TASK", "The task " + key + " is listed more than once.");
            }
        }

        ValidationReport report() {
            return new ValidationReport(errors, warnings);
        }
    }
}
