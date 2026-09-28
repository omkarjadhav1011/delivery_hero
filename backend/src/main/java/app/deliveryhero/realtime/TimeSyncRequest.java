package app.deliveryhero.realtime;

import org.jspecify.annotations.Nullable;

/** A TIME_SYNC request to {@code /app/time-sync} (API section 8.4); a client may omit the field. */
public record TimeSyncRequest(@Nullable Long clientSentAt) {}
