package app.deliveryhero.content;

/** One broken or doubtful rule, naming its field, for example {@code options[1].text} (LLD section 5.3). */
public record Issue(String path, String code, String message) {}
