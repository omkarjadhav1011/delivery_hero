package app.deliveryhero.content;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;

/** A row of {@code run_plan_entries}: one task in one of a plan's lists, at its place (document 10, section 7.4). */
@Entity
@Table(name = "run_plan_entries")
public class RunPlanEntryEntity {

    /** The primary key: a task appears once per plan. */
    @Embeddable
    public record Id(
            @Column(name = "run_plan_id") UUID runPlanId,
            @Column(name = "task_id") UUID taskId) implements Serializable {}

    @EmbeddedId
    private Id id;

    @Enumerated(EnumType.STRING)
    @Column(name = "list_name", nullable = false, length = 12)
    private RunPlanList listName;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    protected RunPlanEntryEntity() {
        this(new UUID(0, 0), new UUID(0, 0), RunPlanList.PRACTICE, 0);
    }

    public RunPlanEntryEntity(UUID runPlanId, UUID taskId, RunPlanList listName, int sortOrder) {
        this.id = new Id(runPlanId, taskId);
        this.listName = listName;
        this.sortOrder = (short) sortOrder;
    }
}
