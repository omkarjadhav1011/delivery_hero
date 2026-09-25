package app.deliveryhero.content;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.common.Phase;
import app.deliveryhero.common.Role;
import app.deliveryhero.common.TaskKind;
import app.deliveryhero.common.TaskType;
import app.deliveryhero.content.ContentValidator.TaskLookup;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Every field rule of SRS section 7.3, in one validator (LLD section 5.3). */
class ContentValidatorTest {

    private final ContentValidator validator = new ContentValidator();

    @ParameterizedTest(name = "{0} minutes")
    @ValueSource(ints = {3, 10})
    @DisplayName("AC-US19-01 round length: 3 and 10 whole minutes are accepted")
    void acceptsRoundLengthsFromThreeToTen(int minutes) {
        assertThat(validator.validateRunPlan(plan(minutes)).errors()).isEmpty();
    }

    @ParameterizedTest(name = "{0} minutes")
    @ValueSource(ints = {2, 11})
    @DisplayName("AC-US19-01 round length: 2 and 11 minutes are refused")
    void refusesRoundLengthsOutsideThreeToTen(int minutes) {
        assertThat(paths(validator.validateRunPlan(plan(minutes)).errors())).containsExactly("roundLengthMinutes");
    }

    @Test
    @DisplayName("Run plan key and name follow the task key rules and 1-60 characters")
    void runPlanKeyAndName() {
        RunPlanDefinition bad = new RunPlanDefinition("Default 5min", "", 5, List.of(), null, Map.of());

        assertThat(paths(validator.validateRunPlan(bad).errors())).containsExactly("key", "name");
    }

    @Test
    @DisplayName("A valid multiple-choice task has no errors and no warnings")
    void validTask() {
        ValidationReport report = validator.validateTask(multipleChoice(options(1)));

        assertThat(report.errors()).isEmpty();
        assertThat(report.warnings()).isEmpty();
    }

    @Test
    @DisplayName("Multiple choice needs exactly one correct option")
    void multipleChoiceNeedsOneCorrectOption() {
        assertThat(paths(validator.validateTask(multipleChoice(options(-1))).errors()))
                .containsExactly("options");
        assertThat(paths(validator
                        .validateTask(multipleChoice(List.of(
                                new MultipleChoiceContent.Option("A", true),
                                new MultipleChoiceContent.Option("B", true))))
                        .errors()))
                .containsExactly("options");
    }

    @Test
    @DisplayName("Multiple choice has 2-4 options of 1-80 characters")
    void multipleChoiceOptionCountAndLength() {
        assertThat(paths(validator
                        .validateTask(multipleChoice(List.of(new MultipleChoiceContent.Option("A", true))))
                        .errors()))
                .containsExactly("options");
        assertThat(paths(validator
                        .validateTask(multipleChoice(List.of(
                                new MultipleChoiceContent.Option("A", true),
                                new MultipleChoiceContent.Option("x".repeat(81), false))))
                        .errors()))
                .containsExactly("options[1].text");
    }

    @Test
    @DisplayName("Task key is 1-40 lowercase letters, digits and hyphens")
    void taskKey() {
        assertThat(paths(validator
                        .validateTask(task("Mgr_plan", TaskKind.SCORED, Phase.PLANNING, mc()))
                        .errors()))
                .containsExactly("key");
        assertThat(paths(validator
                        .validateTask(task("k".repeat(41), TaskKind.SCORED, Phase.PLANNING, mc()))
                        .errors()))
                .containsExactly("key");
    }

    @Test
    @DisplayName("Prompt is 1-200 characters, with a warning above 25 words")
    void prompt() {
        TaskDefinition empty = withPrompt("");
        TaskDefinition wordy = withPrompt("word ".repeat(26).trim());

        assertThat(paths(validator.validateTask(empty).errors())).containsExactly("prompt");
        assertThat(validator.validateTask(wordy).errors()).isEmpty();
        assertThat(paths(validator.validateTask(wordy).warnings())).containsExactly("prompt");
    }

    @Test
    @DisplayName("Code is up to 2,000 characters and 30 lines in a known language, with a warning above 12 lines")
    void codeSnippet() {
        assertThat(validator
                        .validateTask(withCode(new CodeSnippet("java", lines(12))))
                        .warnings())
                .isEmpty();
        assertThat(paths(validator
                        .validateTask(withCode(new CodeSnippet("java", lines(13))))
                        .warnings()))
                .containsExactly("code.text");
        assertThat(paths(validator
                        .validateTask(withCode(new CodeSnippet("java", lines(31))))
                        .errors()))
                .containsExactly("code.text");
        assertThat(paths(validator
                        .validateTask(withCode(new CodeSnippet("java", "x".repeat(2001))))
                        .errors()))
                .containsExactly("code.text");
        assertThat(paths(validator
                        .validateTask(withCode(new CodeSnippet("cobol", "x")))
                        .errors()))
                .containsExactly("code.language");
    }

    @Test
    @DisplayName("Time limit, when set, is 5-60 seconds")
    void timeLimit() {
        assertThat(validator.validateTask(withTimeLimit(5)).errors()).isEmpty();
        assertThat(validator.validateTask(withTimeLimit(60)).errors()).isEmpty();
        assertThat(paths(validator.validateTask(withTimeLimit(4)).errors())).containsExactly("timeLimitSeconds");
        assertThat(paths(validator.validateTask(withTimeLimit(61)).errors())).containsExactly("timeLimitSeconds");
    }

    @Test
    @DisplayName("Explanation is up to 300 characters, and required for scored and incident tasks in the seed file")
    void explanation() {
        TaskDefinition missing = withExplanation(null);
        TaskDefinition practice = new TaskDefinition(
                "practice-1",
                Role.TESTER,
                TaskKind.PRACTICE,
                null,
                TaskType.YES_NO,
                "Warm-up",
                null,
                null,
                new YesNoContent(true),
                null);

        assertThat(paths(
                        validator.validateTask(withExplanation("x".repeat(301))).errors()))
                .containsExactly("explanation");
        assertThat(validator.validateTask(missing).errors()).isEmpty();
        assertThat(paths(validator.validateSeedTask(missing).errors())).containsExactly("explanation");
        assertThat(validator.validateSeedTask(practice).errors()).isEmpty();
    }

    @Test
    @DisplayName("Scored tasks need a phase; practice and incident tasks have none; incident tasks are multiple choice")
    void kindAndPhase() {
        assertThat(paths(validator
                        .validateTask(task("t", TaskKind.SCORED, null, mc()))
                        .errors()))
                .containsExactly("phase");
        assertThat(paths(validator
                        .validateTask(task("t", TaskKind.PRACTICE, Phase.TESTING, mc()))
                        .errors()))
                .containsExactly("phase");
        assertThat(paths(validator
                        .validateTask(new TaskDefinition(
                                "t",
                                Role.DEVELOPER,
                                TaskKind.INCIDENT,
                                null,
                                TaskType.YES_NO,
                                "Down?",
                                null,
                                null,
                                new YesNoContent(true),
                                "Why"))
                        .errors()))
                .containsExactly("type");
    }

    @Test
    @DisplayName("The content must match the task type")
    void contentMatchesType() {
        TaskDefinition mismatch = new TaskDefinition(
                "t", Role.MANAGER, TaskKind.SCORED, Phase.RELEASE, TaskType.YES_NO, "Ship?", null, null, mc(), "Why");

        assertThat(paths(validator.validateTask(mismatch).errors())).containsExactly("type");
    }

    @Test
    @DisplayName("Tap to order has 3-5 items of 1-60 characters, positions 1..n, and a display order that differs")
    void order() {
        assertThat(validator.validateTask(order(2, 1, 3)).errors()).isEmpty();
        assertThat(paths(validator.validateTask(order(1, 2)).errors())).containsExactly("items");
        assertThat(paths(validator.validateTask(order(1, 1, 3)).errors())).containsExactly("items");
        assertThat(paths(validator.validateTask(order(1, 2, 3)).errors())).containsExactly("items");
        assertThat(paths(validator.validateTask(order(2, 1, 4)).errors())).containsExactly("items");
    }

    @Test
    @DisplayName("Problem words mark 1-4 whole words in 1-200 characters")
    void problemWords() {
        assertThat(validator
                        .validateTask(words("Load {{fast}} and be {{nice}} today"))
                        .errors())
                .isEmpty();
        assertThat(paths(validator.validateTask(words("No markers here")).errors()))
                .containsExactly("text");
        assertThat(paths(validator
                        .validateTask(words("{{a}} {{b}} {{c}} {{d}} {{e}}"))
                        .errors()))
                .containsExactly("text");
        assertThat(paths(validator
                        .validateTask(words("Two {{whole words}} here"))
                        .errors()))
                .containsExactly("text");
        assertThat(paths(validator.validateTask(words("Half{{word}} here")).errors()))
                .containsExactly("text");
    }

    @Test
    @DisplayName(
            "Characters have a 1-20 character name, and an intro and three of each reaction line of 1-80 characters")
    void characters() {
        CharacterDefinition good = new CharacterDefinition(
                Role.MANAGER, "Maya", "Quick one!", List.of("a", "b", "c"), List.of("d", "e", "f"));
        CharacterDefinition bad = new CharacterDefinition(
                Role.MANAGER, "x".repeat(21), "", List.of("a", "b"), List.of("d", "e", "x".repeat(81)));

        assertThat(validator.validateCharacter(good).errors()).isEmpty();
        assertThat(paths(validator.validateCharacter(bad).errors()))
                .containsExactly("displayName", "introLine", "correctLines", "wrongLines[2]");
    }

    @Test
    @DisplayName("Run plan lists: keys exist, each list holds the right kind, nothing twice, no empty phase")
    void runPlanLists() {
        Map<String, TaskLookup> tasks = Map.of(
                "p1", new TaskLookup(TaskKind.PRACTICE, null, TaskType.YES_NO),
                "inc", new TaskLookup(TaskKind.INCIDENT, null, TaskType.MULTIPLE_CHOICE),
                "plan-1", new TaskLookup(TaskKind.SCORED, Phase.PLANNING, TaskType.YES_NO),
                "dev-1", new TaskLookup(TaskKind.SCORED, Phase.DEVELOPMENT, TaskType.YES_NO),
                "test-1", new TaskLookup(TaskKind.SCORED, Phase.TESTING, TaskType.YES_NO),
                "rel-1", new TaskLookup(TaskKind.SCORED, Phase.RELEASE, TaskType.YES_NO));
        RunPlanDefinition good = new RunPlanDefinition(
                "default-5min",
                "Default",
                5,
                List.of("p1"),
                "inc",
                Map.of(
                        Phase.PLANNING, List.of("plan-1"),
                        Phase.DEVELOPMENT, List.of("dev-1"),
                        Phase.TESTING, List.of("test-1"),
                        Phase.RELEASE, List.of("rel-1")));
        RunPlanDefinition bad = new RunPlanDefinition(
                "default-5min",
                "Default",
                5,
                List.of("plan-1"),
                "p1",
                Map.of(
                        Phase.PLANNING, List.of("plan-1", "missing"),
                        Phase.DEVELOPMENT, List.of("test-1"),
                        Phase.TESTING, List.of()));

        assertThat(validator.validateRunPlanLists(good, tasks::get).errors()).isEmpty();
        assertThat(paths(validator.validateRunPlanLists(bad, tasks::get).errors()))
                .containsExactly(
                        "practice[0]",
                        "incident",
                        "phases.PLANNING[0]",
                        "phases.PLANNING[1]",
                        "phases.DEVELOPMENT[0]",
                        "phases.TESTING",
                        "phases.RELEASE");
    }

    private static RunPlanDefinition plan(int minutes) {
        return new RunPlanDefinition("default-5min", "Default 5-minute plan", minutes, List.of(), null, Map.of());
    }

    private static List<String> paths(List<Issue> issues) {
        return issues.stream().map(Issue::path).toList();
    }

    private static MultipleChoiceContent mc() {
        return new MultipleChoiceContent(options(0));
    }

    /** Four options with the one at {@code correct} marked correct; -1 marks none. */
    private static List<MultipleChoiceContent.Option> options(int correct) {
        return List.of(
                new MultipleChoiceContent.Option("A", correct == 0),
                new MultipleChoiceContent.Option("B", correct == 1),
                new MultipleChoiceContent.Option("C", correct == 2),
                new MultipleChoiceContent.Option("D", correct == 3));
    }

    private static TaskDefinition task(String key, TaskKind kind, @Nullable Phase phase, TaskContent content) {
        TaskType type = switch (content) {
            case MultipleChoiceContent c -> TaskType.MULTIPLE_CHOICE;
            case YesNoContent c -> TaskType.YES_NO;
            case OrderContent c -> TaskType.ORDER;
            case ProblemWordsContent c -> TaskType.PROBLEM_WORDS;
        };
        return new TaskDefinition(key, Role.MANAGER, kind, phase, type, "Your first move?", null, null, content, "Why");
    }

    private static TaskDefinition multipleChoice(List<MultipleChoiceContent.Option> options) {
        return task("mgr-plan-001", TaskKind.SCORED, Phase.PLANNING, new MultipleChoiceContent(options));
    }

    private static TaskDefinition withPrompt(String prompt) {
        return new TaskDefinition(
                "t",
                Role.MANAGER,
                TaskKind.SCORED,
                Phase.PLANNING,
                TaskType.MULTIPLE_CHOICE,
                prompt,
                null,
                null,
                mc(),
                "Why");
    }

    private static TaskDefinition withCode(CodeSnippet code) {
        return new TaskDefinition(
                "t",
                Role.DEVELOPER,
                TaskKind.SCORED,
                Phase.DEVELOPMENT,
                TaskType.MULTIPLE_CHOICE,
                "Bug?",
                code,
                null,
                mc(),
                "Why");
    }

    private static TaskDefinition withTimeLimit(int seconds) {
        return new TaskDefinition(
                "t",
                Role.MANAGER,
                TaskKind.SCORED,
                Phase.PLANNING,
                TaskType.MULTIPLE_CHOICE,
                "Move?",
                null,
                seconds,
                mc(),
                "Why");
    }

    private static TaskDefinition withExplanation(@Nullable String explanation) {
        return new TaskDefinition(
                "t",
                Role.MANAGER,
                TaskKind.SCORED,
                Phase.PLANNING,
                TaskType.MULTIPLE_CHOICE,
                "Move?",
                null,
                null,
                mc(),
                explanation);
    }

    private static TaskDefinition order(int... positions) {
        List<OrderContent.Item> items = java.util.stream.IntStream.range(0, positions.length)
                .mapToObj(i -> new OrderContent.Item("Item " + i, positions[i]))
                .toList();
        return task("t", TaskKind.SCORED, Phase.TESTING, new OrderContent(items));
    }

    private static TaskDefinition words(String text) {
        return task("t", TaskKind.SCORED, Phase.PLANNING, new ProblemWordsContent(text, false));
    }

    private static String lines(int count) {
        return String.join("\n", java.util.Collections.nCopies(count, "x"));
    }
}
