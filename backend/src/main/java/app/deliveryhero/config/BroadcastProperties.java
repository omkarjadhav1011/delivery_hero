package app.deliveryhero.config;

import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Broadcast settings under {@code dh.broadcast} (LLD section 5.13).
 *
 * @param batchInterval how often the session sends the projector and admin batches, the {@code FLUSH} timer (DEC-128)
 */
@Validated
@ConfigurationProperties("dh.broadcast")
public record BroadcastProperties(
        @DefaultValue("500ms") @DurationMin(millis = 1) Duration batchInterval) {}
