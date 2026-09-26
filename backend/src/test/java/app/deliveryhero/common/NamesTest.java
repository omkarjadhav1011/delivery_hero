package app.deliveryhero.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Names are normalized to NFC, tidied and checked against BR-16 (DEC-120; Test Plan section 9.2). */
class NamesTest {

    private static final String PRECOMPOSED_JOSE = "José";
    private static final String COMBINING_JOSE = "José";

    @Test
    @DisplayName("AC-US02-01 spaces tidied: \"  Priya   S \" becomes \"Priya S\"")
    void spacesAreTidied() {
        String name = Names.normalize("  Priya   S ");

        assertThat(name).isEqualTo("Priya S");
        assertThat(Names.isValid(name)).isTrue();
    }

    @ParameterizedTest(name = "\"{0}\" is refused")
    @ValueSource(strings = {"", "   ", "Abcdefghijklmnopqrstu", "priya@home"})
    @DisplayName("AC-US02-02 invalid names: empty, 21 characters, or containing \"@\"")
    void invalidNamesAreRefused(String raw) {
        assertThat(Names.isValid(Names.normalize(raw))).isFalse();
    }

    @Test
    @DisplayName("AC-US02-05 accented letters: a combining \"José\" is stored in NFC, the same as a precomposed one")
    void combiningAccentsBecomeNfc() {
        String combining = Names.normalize(COMBINING_JOSE);

        assertThat(combining).isEqualTo(PRECOMPOSED_JOSE).hasSize(4);
        assertThat(Names.normalize(PRECOMPOSED_JOSE)).isEqualTo(PRECOMPOSED_JOSE);
        assertThat(Names.isValid(combining)).isTrue();
    }

    @ParameterizedTest(name = "\"{0}\" is accepted")
    @ValueSource(strings = {"Zoë", "O'Brien", "Anne-Marie", "A.J.", "李明", "Priya 2", "Abcdefghijklmnopqrst"})
    @DisplayName(
            "Test Plan 9.2: letters in any language, digits, spaces, hyphens, apostrophes, full stops, 20 characters")
    void validNamesAreAccepted(String raw) {
        assertThat(Names.isValid(Names.normalize(raw))).isTrue();
    }

    @ParameterizedTest(name = "\"{0}\" is refused")
    @ValueSource(strings = {"Sam 😀", "<b>Sam</b>", "Sam\tK", "Sam_K"})
    @DisplayName("Test Plan 9.2: an emoji, markup and other characters are refused")
    void otherCharactersAreRefused(String raw) {
        assertThat(Names.isValid(Names.normalize(raw))).isFalse();
    }

    @Test
    @DisplayName("A letter that needs a combining mark even in NFC counts as a letter (DEC-120)")
    void combiningMarksThatStayAreLetters() {
        // Devanagari "ki": the vowel sign is a combining mark with no precomposed form
        String name = Names.normalize("कि");

        assertThat(Normalizer.isNormalized(name, Normalizer.Form.NFC)).isTrue();
        assertThat(Names.isValid(name)).isTrue();
    }
}
