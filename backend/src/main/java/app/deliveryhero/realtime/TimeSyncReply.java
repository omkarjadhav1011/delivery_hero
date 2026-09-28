package app.deliveryhero.realtime;

/** The TIME_SYNC reply on {@code /user/queue/time-sync} (API section 8.5): the request's send time, and the server's. */
public record TimeSyncReply(String type, long serverTime, long clientSentAt) {}
