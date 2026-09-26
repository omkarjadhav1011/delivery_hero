package app.deliveryhero.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.json.JsonMapper;

/** The {@code answer} object of ANSWER_SUBMIT (API section 8.4). */
class AnswerPayloadTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @ParameterizedTest(name = "{0}")
    @CsvSource(
            delimiter = '|',
            value = {
                "CHOICE  | {\"kind\": \"CHOICE\", \"optionIndex\": 2}",
                "YES_NO  | {\"kind\": \"YES_NO\", \"yes\": true}",
                "ORDER   | {\"kind\": \"ORDER\", \"itemIndexes\": [2, 0, 1]}",
                "WORDS   | {\"kind\": \"WORDS\", \"tokenIndexes\": [4, 9]}"
            })
    void readsEachKind(String kind, String body) {
        AnswerPayload expected = switch (kind) {
            case "CHOICE" -> new ChoiceAnswer(2);
            case "YES_NO" -> new YesNoAnswer(true);
            case "ORDER" -> new OrderAnswer(List.of(2, 0, 1));
            default -> new WordsAnswer(Set.of(4, 9));
        };

        assertThat(json.readValue(body, AnswerPayload.class)).isEqualTo(expected);
    }
}
