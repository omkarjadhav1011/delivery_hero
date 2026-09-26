package app.deliveryhero.config;

import java.net.URI;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * The site's public address under {@code dh.public-base-url}, such as {@code https://hero.example.org}: an http or https
 * URL with no path, or blank before it's configured. A malformed value stops the application at startup.
 *
 * @param publicBaseUrl the address players and the projector open
 */
@Validated
@ConfigurationProperties("dh")
public record SiteProperties(@DefaultValue("") String publicBaseUrl) {

    public SiteProperties {
        publicBaseUrl = publicBaseUrl.strip();
        if (!publicBaseUrl.isEmpty()) {
            URI uri = URI.create(publicBaseUrl);
            boolean web = "http".equals(uri.getScheme()) || "https".equals(uri.getScheme());
            String path = uri.getRawPath();
            if (!web || uri.getHost() == null || (path != null && !path.isEmpty() && !path.equals("/"))) {
                throw new IllegalArgumentException("dh.public-base-url must be an http or https address with no path");
            }
        }
    }

    /** The address a phone opens to join the game with this code (API section 7.7). */
    public String joinUrl(String code) {
        return withoutTrailingSlash() + "/join?code=" + code;
    }

    /** The projector's address with the game's key, shown only in the admin panel (FR-052, API section 7.7). */
    public String projectorUrl(String projectorKey) {
        return withoutTrailingSlash() + "/screen?key=" + projectorKey;
    }

    private String withoutTrailingSlash() {
        return publicBaseUrl.replaceAll("/+$", "");
    }

    /** The site's origin, scheme, host and any port, as a browser sends it; empty when no address is configured. */
    public Optional<String> origin() {
        if (publicBaseUrl.isEmpty()) {
            return Optional.empty();
        }
        URI uri = URI.create(publicBaseUrl);
        return Optional.of(uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort()));
    }
}
