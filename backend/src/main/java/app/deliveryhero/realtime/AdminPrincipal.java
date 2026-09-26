package app.deliveryhero.realtime;

/** An admin panel whose WebSocket handshake carried an authenticated admin session. */
public record AdminPrincipal() implements ClientPrincipal {

    @Override
    public String getName() {
        return "admin";
    }
}
