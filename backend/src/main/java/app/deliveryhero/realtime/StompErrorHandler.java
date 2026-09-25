package app.deliveryhero.realtime;

import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

/** Turns a {@link StompRefusal} into an ERROR frame carrying only its code, so no game data is sent (API 8.1). */
public class StompErrorHandler extends StompSubProtocolErrorHandler {

    private static final byte[] EMPTY = new byte[0];

    @Override
    public @Nullable Message<byte[]> handleClientMessageProcessingError(
            @Nullable Message<byte[]> clientMessage, Throwable ex) {
        StompRefusal refusal = findRefusal(ex);
        if (refusal == null) {
            return super.handleClientMessageProcessingError(clientMessage, ex);
        }
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(refusal.code().name());
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(EMPTY, accessor.getMessageHeaders());
    }

    private static @Nullable StompRefusal findRefusal(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof StompRefusal refusal) {
                return refusal;
            }
        }
        return null;
    }
}
