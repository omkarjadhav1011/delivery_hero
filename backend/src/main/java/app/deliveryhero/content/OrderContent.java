package app.deliveryhero.content;

import java.util.List;

/** Tap-to-order items, listed in display order (LLD section 5.2). */
public record OrderContent(List<Item> items) implements TaskContent {

    public record Item(String text, int correctPosition) {}
}
