package app.deliveryhero.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The site's origin, which alone may open the STOMP endpoint (LLD section 5.6). */
class SitePropertiesTest {

    @Test
    @DisplayName("The origin is the scheme, host and any port of the public base URL")
    void originOfTheBaseUrl() {
        assertThat(new SiteProperties("https://hero.example.org/").origin()).contains("https://hero.example.org");
        assertThat(new SiteProperties("http://localhost:8090").origin()).contains("http://localhost:8090");
        assertThat(new SiteProperties(" ").origin()).isEmpty();
    }

    @Test
    @DisplayName("A base URL with a path, or that isn't http or https, is rejected")
    void malformedBaseUrlsAreRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> new SiteProperties("https://hero.example.org/app"));
        assertThatIllegalArgumentException().isThrownBy(() -> new SiteProperties("ftp://hero.example.org"));
        assertThatIllegalArgumentException().isThrownBy(() -> new SiteProperties("hero.example.org"));
    }
}
