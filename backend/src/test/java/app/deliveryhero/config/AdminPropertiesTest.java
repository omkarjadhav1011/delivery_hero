package app.deliveryhero.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Validation of {@code dh.admin.password-hash}: a bcrypt hash of cost 12 or more (DEC-98). */
class AdminPropertiesTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    // Generated for this test only; not any real password's hash
    private static final String COST_12 = "$2y$12$" + "a".repeat(53);
    private static final String COST_10 = "$2y$10$" + "a".repeat(53);

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @Test
    @DisplayName("A bcrypt hash of cost 12 is accepted")
    void acceptsCostTwelve() {
        assertThat(VALIDATOR.validate(new AdminProperties(COST_12))).isEmpty();
    }

    @Test
    @DisplayName("A bcrypt hash below cost 12 is rejected without echoing the hash")
    void rejectsWeakerHash() {
        Set<ConstraintViolation<AdminProperties>> violations = VALIDATOR.validate(new AdminProperties(COST_10));

        assertThat(violations)
                .singleElement()
                .satisfies(violation -> assertThat(violation.getMessage())
                        .contains("cost 12 or more")
                        .doesNotContain(COST_10));
    }

    @Test
    @DisplayName("A missing hash is rejected")
    void rejectsMissingHash() {
        assertThat(VALIDATOR.validate(new AdminProperties(""))).isNotEmpty();
    }
}
