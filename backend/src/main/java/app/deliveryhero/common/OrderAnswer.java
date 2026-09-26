package app.deliveryhero.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** An ordering answer: every item's display index, in the chosen order ({@code itemIndexes} on the wire). */
public record OrderAnswer(@JsonProperty("itemIndexes") List<Integer> itemIndexesInChosenOrder)
        implements AnswerPayload {}
