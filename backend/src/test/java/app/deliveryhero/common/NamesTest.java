package app.deliveryhero.common;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.engine.NameRegistry;
import java.text.Normalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Names are normalized to NFC, tidied, checked against BR-16 and made unique in the game (DEC-120; Test Plan section
 * 9.2).
 */
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

    @Test
    @DisplayName("AC-US02-03 duplicate: after \"Rahul\" and \"Rahul 2\", \"rahul\" joins as \"rahul 3\"")
    void duplicatesIgnoreCaseAndTakeTheLowestFreeNumber() {
        NameRegistry registry = new NameRegistry();

        assertThat(registry.unique("Rahul")).isEqualTo("Rahul");
        assertThat(registry.unique("Rahul 2")).isEqualTo("Rahul 2");
        assertThat(registry.unique("rahul")).isEqualTo("rahul 3");
    }

    @Test
    @DisplayName(
            "AC-US02-04 duplicate at 20 characters: \"Alexandria Constance\" again becomes \"Alexandria Constan 2\"")
    void aDuplicateAtTwentyCharactersShortensTheBase() {
        NameRegistry registry = new NameRegistry();
        registry.unique("Alexandria Constance");

        assertThat(registry.unique("Alexandria Constance")).isEqualTo("Alexandria Constan 2");
    }

    @Test
    @DisplayName("AC-US02-05 accented letters: a precomposed and a combining \"José\" give \"José\" and \"José 2\"")
    void nfcFormsAreTheSameName() {
        NameRegistry registry = new NameRegistry();

        assertThat(registry.unique(Names.normalize(PRECOMPOSED_JOSE))).isEqualTo(PRECOMPOSED_JOSE);
        assertThat(registry.unique(Names.normalize(COMBINING_JOSE))).isEqualTo(PRECOMPOSED_JOSE + " 2");
    }

    @Test
    @DisplayName("Test Plan 9.2: \"Sam\", then \"sam\", join as \"Sam\" and \"sam 2\"")
    void caseOnlyDuplicate() {
        NameRegistry registry = new NameRegistry();

        assertThat(registry.unique("Sam")).isEqualTo("Sam");
        assertThat(registry.unique("sam")).isEqualTo("sam 2");
    }

    @Test
    @DisplayName(
            "A base shortened just after a space drops the space: \"Alexandria Consta c\" becomes \"Alexandria Consta 2\"")
    void shortenedBasesDropATrailingSpace() {
        NameRegistry registry = new NameRegistry();
        registry.unique("Alexandria Consta c");

        String second = registry.unique("Alexandria Consta c");

        assertThat(second).isEqualTo("Alexandria Consta 2");
        assertThat(Names.isValid(second)).isTrue();
    }

    @Test
    @DisplayName(
            "A two-digit number shortens the base further: the eleventh \"Alexandria Constan c\" fits 20 characters")
    void twoDigitNumbersStillFit() {
        NameRegistry registry = new NameRegistry();
        for (int i = 0; i < 10; i++) {
            registry.unique("Alexandria Constan c");
        }

        assertThat(registry.unique("Alexandria Constan c")).isEqualTo("Alexandria Consta 11");
    }
}
