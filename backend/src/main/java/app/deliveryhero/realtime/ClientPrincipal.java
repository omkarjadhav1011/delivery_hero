package app.deliveryhero.realtime;

import java.security.Principal;

/** Who a STOMP connection belongs to, set when CONNECT is accepted (LLD section 5.6). */
public sealed interface ClientPrincipal extends Principal permits PlayerPrincipal, ProjectorPrincipal, AdminPrincipal {}
