package app.deliveryhero.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Persistable;

/** A row of {@code run_plans}, a game template (document 10, section 7.3). Its lists are {@link RunPlanEntryEntity}. */
@Entity
@Table(name = "run_plans")
public class RunPlanEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "plan_key", nullable = false, unique = true, length = 40)
    private String planKey;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "round_length_minutes", nullable = false)
    private short roundLengthMinutes;

    @Column(name = "incident_task_id")
    private @Nullable UUID incidentTaskId;

    @Version
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** True until the row is saved or loaded, so a save inserts without first selecting (assigned keys). */
    @Transient
    private boolean isNew;

    protected RunPlanEntity() {
        this(new UUID(0, 0), "", Instant.EPOCH);
        this.isNew = false;
    }

    public RunPlanEntity(UUID id, String planKey, Instant now) {
        this.id = id;
        this.planKey = planKey;
        this.name = "";
        this.createdAt = now;
        this.updatedAt = now;
        this.isNew = true;
    }

    /** Copies the name, round length and incident task; the lists are separate rows. */
    public void apply(RunPlanDefinition plan, @Nullable UUID incidentTaskId, Instant now) {
        this.name = plan.name();
        this.roundLengthMinutes = (short) plan.roundLengthMinutes();
        this.incidentTaskId = incidentTaskId;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public String planKey() {
        return planKey;
    }

    public String name() {
        return name;
    }

    public int roundLengthMinutes() {
        return roundLengthMinutes;
    }

    @Override
    public UUID getId() {
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
