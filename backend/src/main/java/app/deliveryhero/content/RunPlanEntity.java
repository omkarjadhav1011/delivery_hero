package app.deliveryhero.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** A row of {@code run_plans}, a game template (document 10, section 7.3). Its lists are {@link RunPlanEntryEntity}. */
@Entity
@Table(name = "run_plans")
public class RunPlanEntity {

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

    protected RunPlanEntity() {
        this(new UUID(0, 0), "", Instant.EPOCH);
    }

    public RunPlanEntity(UUID id, String planKey, Instant now) {
        this.id = id;
        this.planKey = planKey;
        this.name = "";
        this.createdAt = now;
        this.updatedAt = now;
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

    public int roundLengthMinutes() {
        return roundLengthMinutes;
    }
}
