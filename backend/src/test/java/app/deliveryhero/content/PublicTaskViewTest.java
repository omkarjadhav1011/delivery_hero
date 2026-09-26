package app.deliveryhero.content;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.common.Phase;
import app.deliveryhero.common.Role;
import app.deliveryhero.common.TaskKind;
import app.deliveryhero.common.TaskType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** The one task shape that leaves the server before Results (LLD section 5.3, DEC-130, API section 9.1). */
class PublicTaskViewTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    @DisplayName("Multiple choice shows option texts in display order, without correct flags")
    void multipleChoice() {
        PublicTaskView view = PublicTaskView.of(
                task(
                        TaskType.MULTIPLE_CHOICE,
                        new MultipleChoiceContent(List.of(
                                new MultipleChoiceContent.Option("B", false),
                                new MultipleChoiceContent.Option("A", true)))),
                "Tess");

        assertThat(view.options()).containsExactly("B", "A");
        assertThat(view.items()).isNull();
        assertThat(view.tokens()).isNull();
        assertThat(json.writeValueAsString(view)).doesNotContain("correct").doesNotContain("true");
    }

    @Test
    @DisplayName("Yes/no carries no type-specific fields and no answer")
    void yesNo() {
        PublicTaskView view = PublicTaskView.of(task(TaskType.YES_NO, new YesNoContent(true)), "Tess");

        assertThat(view.options()).isNull();
        assertThat(view.monospace()).isNull();
        assertThat(view.timeLimitMs()).isEqualTo(8000);
        assertThat(json.writeValueAsString(view)).doesNotContain("answer").doesNotContain("true");
    }

    @Test
    @DisplayName("Tap to order shows item texts in display order, without positions")
    void order() {
        PublicTaskView view = PublicTaskView.of(
                task(
                        TaskType.ORDER,
                        new OrderContent(List.of(
                                new OrderContent.Item("Integration", 2),
                                new OrderContent.Item("Unit", 1),
                                new OrderContent.Item("End-to-end", 3)))),
                "Tess");

        assertThat(view.items()).containsExactly("Integration", "Unit", "End-to-end");
        assertThat(json.writeValueAsString(view)).doesNotContain("correctPosition");
    }

    @Test
    @DisplayName("Problem words are tokens split at whitespace with the markers removed, not which are problems")
    void problemWords() {
        PublicTaskView view = PublicTaskView.of(
                task(TaskType.PROBLEM_WORDS, new ProblemWordsContent("  if (x  {{==}} null)\n{{return}} ", true)),
                "Tess");

        assertThat(view.tokens()).containsExactly("if", "(x", "==", "null)", "return");
        assertThat(view.monospace()).isTrue();
        assertThat(json.writeValueAsString(view)).doesNotContain("{{").doesNotContain("markedText");
    }

    private static TaskDefinition task(TaskType type, TaskContent content) {
        return new TaskDefinition(
                "tst-test-01", Role.TESTER, TaskKind.SCORED, Phase.TESTING, type, "Which?", null, null, content, "Why");
    }
}
