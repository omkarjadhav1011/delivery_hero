package app.deliveryhero.content;

import app.deliveryhero.common.Phase;
import app.deliveryhero.common.TaskKind;
import app.deliveryhero.common.TaskType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Every field rule of SRS section 7.3 for characters, tasks and run plans, in one place (LLD section 5.3). The seed
 * loader also checks run plan lists here, with the same errors as {@code tools/validate_seed.py}.
 */
@Component
public class ContentValidator {

    private static final Pattern KEY = Pattern.compile("^[a-z0-9-]{1,40}$");
    private static final Pattern MARKED_WORD = Pattern.compile("^\\{\\{[^{}\\s]+}}$");
    private static final Set<String> LANGUAGES =
            Set.of("text", "java", "javascript", "typescript", "sql", "json", "python", "shell");
    private static final int PROMPT_WORDS_WARNING = 25;
    private static final int CODE_LINES_WARNING = 12;

    /** What the run plan list checks need to know about a task named by key. */
    public record TaskLookup(TaskKind kind, @Nullable Phase phase, TaskType type) {}

    public ValidationReport validateCharacter(CharacterDefinition character) {
        Issues issues = new Issues();
        issues.length("displayName", character.displayName(), 1, 20);
        issues.length("introLine", character.introLine(), 1, 80);
        issues.reactionLines("correctLines", character.correctLines());
        issues.reactionLines("wrongLines", character.wrongLines());
        return issues.report();
    }

    public ValidationReport validateTask(TaskDefinition task) {
        Issues issues = new Issues();
        checkTask(task, issues);
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
            issues.error("roundLengthMinutes", "RANGE", "The round length must be 3 to 10 whole minutes.");
        }
        return issues.report();
    }

    /**
     * The run plan's lists against the tasks they name: every key exists, each list holds tasks of its kind and
     * phase, no task is listed twice and no phase is empty (SRS section 7.4, BR-13 errors).
     */
    public ValidationReport validateRunPlanLists(RunPlanDefinition plan, Function<String, @Nullable TaskLookup> tasks) {
        Issues issues = new Issues();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < plan.practice().size(); i++) {
            String key = plan.practice().get(i);
            issues.entry(
                    "practice[" + i + "]",
                    key,
                    tasks.apply(key),
                    seen,
                    t -> t.kind() == TaskKind.PRACTICE,
                    "The practice list takes only practice tasks.");
        }
        String incident = plan.incident();
        if (incident != null) {
            issues.entry(
                    "incident",
                    incident,
                    tasks.apply(incident),
                    seen,
                    t -> t.kind() == TaskKind.INCIDENT && t.type() == TaskType.MULTIPLE_CHOICE,
                    "The incident must be a multiple-choice task of kind INCIDENT.");
        }
        for (Phase phase : Phase.values()) {
            String path = "phases." + phase;
            List<String> keys = plan.phases().getOrDefault(phase, List.of());
            if (keys.isEmpty()) {
                issues.error(path, "EMPTY", "The phase " + phase + " is empty.");
            }
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                issues.entry(
                        path + "[" + i + "]",
                        key,
                        tasks.apply(key),
                        seen,
                        t -> t.kind() == TaskKind.SCORED && t.phase() == phase,
                        "The " + phase + " list takes only scored tasks of that phase.");
            }
        }
        return issues.report();
    }

    private static void checkTask(TaskDefinition task, Issues issues) {
        issues.key("key", task.key());
        if ((task.kind() == TaskKind.SCORED) != (task.phase() != null)) {
            issues.error("phase", "PHASE", "Scored tasks need a phase; practice and incident tasks have none.");
        }
        if (!matches(task.type(), task.content())) {
            issues.error("type", "TYPE_MISMATCH", "The content doesn't match the task type " + task.type() + ".");
        } else if (task.kind() == TaskKind.INCIDENT && task.type() != TaskType.MULTIPLE_CHOICE) {
            issues.error("type", "INCIDENT_TYPE", "Incident tasks must be multiple choice.");
        }
        if (issues.length("prompt", task.prompt(), 1, 200) && words(task.prompt()) > PROMPT_WORDS_WARNING) {
            issues.warning("prompt", "LONG_PROMPT", "The prompt has more than 25 words.");
        }
        CodeSnippet code = task.code();
        if (code != null) {
            checkCode(code, issues);
        }
        Integer limit = task.timeLimitSeconds();
        if (limit != null && (limit < 5 || limit > 60)) {
            issues.error("timeLimitSeconds", "RANGE", "The time limit must be 5 to 60 seconds.");
        }
        String explanation = task.explanation();
        if (explanation != null && explanation.length() > 300) {
            issues.error("explanation", "LENGTH", "The explanation must be at most 300 characters.");
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
            issues.error("code.language", "LANGUAGE", "The code language must be one of " + LANGUAGES + ".");
        }
        long lines = code.text().chars().filter(ch -> ch == '\n').count() + 1;
        if (code.text().length() > 2000 || lines > 30) {
            issues.error("code.text", "LENGTH", "The code must be at most 2,000 characters and 30 lines.");
        } else if (lines > CODE_LINES_WARNING) {
            issues.warning("code.text", "LONG_CODE", "The code has more than 12 lines.");
        }
    }

    private static void checkMultipleChoice(MultipleChoiceContent content, Issues issues) {
        List<MultipleChoiceContent.Option> options = content.options();
        if (options.size() < 2 || options.size() > 4) {
            issues.error("options", "COUNT", "Multiple choice needs 2 to 4 options.");
        }
        for (int i = 0; i < options.size(); i++) {
            issues.length("options[" + i + "].text", options.get(i).text(), 1, 80);
        }
        if (options.stream().filter(MultipleChoiceContent.Option::correct).count() != 1) {
            issues.error("options", "CORRECT_COUNT", "Multiple choice needs exactly one correct option.");
        }
    }

    private static void checkOrder(OrderContent content, Issues issues) {
        List<OrderContent.Item> items = content.items();
        for (int i = 0; i < items.size(); i++) {
            issues.length("items[" + i + "].text", items.get(i).text(), 1, 60);
        }
        if (items.size() < 3 || items.size() > 5) {
            issues.error("items", "COUNT", "Tap to order needs 3 to 5 items.");
            return;
        }
        List<Integer> positions =
                items.stream().map(OrderContent.Item::correctPosition).toList();
        List<Integer> expected =
                java.util.stream.IntStream.rangeClosed(1, items.size()).boxed().toList();
        if (!positions.stream().sorted().toList().equals(expected)) {
            issues.error(
                    "items",
                    "POSITIONS",
                    "Each item needs a different correct position from 1 to " + items.size() + ".");
        } else if (positions.equals(expected)) {
            issues.error("items", "SAME_ORDER", "The display order must differ from the correct order.");
        }
    }

    private static void checkProblemWords(ProblemWordsContent content, Issues issues) {
        if (!issues.length("text", content.markedText(), 1, 200)) {
            return;
        }
        List<String> marked = List.of(content.markedText().trim().split("\\s+")).stream()
                .filter(token -> token.contains("{{") || token.contains("}}"))
                .toList();
        if (marked.stream().anyMatch(token -> !MARKED_WORD.matcher(token).matches())) {
            issues.error("text", "MARKER", "Each {{marker}} must wrap exactly one whole word.");
        } else if (marked.isEmpty() || marked.size() > 4) {
            issues.error("text", "MARKER_COUNT", "Mark 1 to 4 problem words.");
        }
    }

    private static boolean matches(TaskType type, TaskContent content) {
        return switch (content) {
            case MultipleChoiceContent c -> type == TaskType.MULTIPLE_CHOICE;
            case YesNoContent c -> type == TaskType.YES_NO;
            case OrderContent c -> type == TaskType.ORDER;
            case ProblemWordsContent c -> type == TaskType.PROBLEM_WORDS;
        };
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
                error(path, "KEY", "Keys are 1 to 40 lowercase letters, digits and hyphens.");
            }
        }

        boolean length(String path, String text, int min, int max) {
            if (text.length() < min || text.length() > max) {
                error(path, "LENGTH", "Must be " + min + " to " + max + " characters.");
                return false;
            }
            return true;
        }

        void reactionLines(String path, List<String> lines) {
            if (lines.size() != 3) {
                error(path, "COUNT", "Exactly 3 lines are needed.");
                return;
            }
            for (int i = 0; i < lines.size(); i++) {
                length(path + "[" + i + "]", lines.get(i), 1, 80);
            }
        }

        void entry(
                String path,
                String key,
                @Nullable TaskLookup task,
                Set<String> seen,
                java.util.function.Predicate<TaskLookup> fits,
                String wrongList) {
            if (task == null) {
                error(path, "UNKNOWN_KEY", "No task has the key " + key + ".");
            } else if (!seen.add(key)) {
                error(path, "DUPLICATE", "The task " + key + " is listed more than once.");
            } else if (!fits.test(task)) {
                error(path, "WRONG_LIST", wrongList);
            }
        }

        ValidationReport report() {
            return new ValidationReport(errors, warnings);
        }
    }
}
