package app.deliveryhero.common;

import org.jspecify.annotations.Nullable;

/**
 * The stable error codes of REST responses, with their HTTP status, title and user-facing message (API section 6.2,
 * LLD section 5.12, DEC-144). The frontend chooses what to show from the code, never from the title.
 */
public enum ApiErrorCode {
    GAME_NOT_ACTIVE(404, "Game not active", "This game link isn't active. Ask the host for the current link."),
    LOBBY_NOT_OPEN(409, "Lobby not open", "The lobby isn't open yet. Hang tight!"),
    JOINING_CLOSED(409, "Joining closed", "Joining has closed for this round. Enjoy the show on the big screen!"),
    GAME_FULL(409, "Game full", "This game is full."),
    INVALID_NAME(
            422,
            "Invalid name",
            "Names can use letters, numbers, spaces, hyphens, apostrophes and full stops, up to 20 characters."),
    RATE_LIMITED(429, "Rate limited", "Too many tries. Please wait a moment and try again."),
    /** No valid admin session, or a failed login; the login screen words its own message (document 12, A-01). */
    UNAUTHENTICATED(401, "Unauthenticated", null),
    /** Content breaks SRS 7.3; {@code errors} lists each issue, shown next to its field. */
    VALIDATION_FAILED(422, "Validation failed", null),
    /** Deleting a task a run plan uses; the detail names the plans and {@code errors} lists them (FR-071). */
    TASK_IN_USE(409, "Task in use", "This task is used by: …"),
    NOT_FOUND(404, "Not found", null);
    // TODO(US-53): EDIT_CONFLICT and the host codes of API section 6.2 arrive with the endpoints that return them

    private final int status;
    private final String title;
    private final @Nullable String detail;

    ApiErrorCode(int status, String title, @Nullable String detail) {
        this.status = status;
        this.title = title;
        this.detail = detail;
    }

    public int status() {
        return status;
    }

    public String title() {
        return title;
    }

    /** The exact user-facing message from the SRS, where one exists (API section 6.1). */
    public @Nullable String detail() {
        return detail;
    }
}
