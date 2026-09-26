package app.deliveryhero.content;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/** A row of {@code run_plan_entries}: one task in one of a plan's lists, at its place (document 10, section 7.4). */
@Entity
@Table(name = "run_plan_entries")
public class RunPlanEntryEntity implements Persistable<RunPlanEntryEntity.Id> {

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

    /** True until the row is saved or loaded, so a save inserts without first selecting (assigned keys). */
    @Transient
    private boolean isNew;

    protected RunPlanEntryEntity() {
        this(new UUID(0, 0), new UUID(0, 0), RunPlanList.PRACTICE, 0);
        this.isNew = false;
    }

    public RunPlanEntryEntity(UUID runPlanId, UUID taskId, RunPlanList listName, int sortOrder) {
        this.id = new Id(runPlanId, taskId);
        this.listName = listName;
        this.sortOrder = (short) sortOrder;
        this.isNew = true;
    }

    public UUID taskId() {
        return id.taskId();
    }

    public RunPlanList listName() {
        return listName;
    }

    public int sortOrder() {
        return sortOrder;
    }

    @Override
    public Id getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markStored() {
        this.isNew = false;
    }
}
