package app.deliveryhero.realtime;

/**
 * A refused STOMP frame. The client gets an ERROR frame whose {@code message} header is the code, with no body, and
 * the connection closes (API section 8.1).
 */
public final class StompRefusal extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** The code in the ERROR frame's {@code message} header. */
    public enum Code {
        /** CONNECT without valid credentials (API section 8.1). */
        UNAUTHORIZED,
        /** A SUBSCRIBE or SEND the connection's role may not make (API section 8.2). */
        FORBIDDEN
    }

    private final Code code;

    public StompRefusal(Code code) {
        super(code.name(), null, false, false);
        this.code = code;
    }

    public Code code() {
        return code;
    }
}
