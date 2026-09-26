package app.deliveryhero.lifecycle;

import app.deliveryhero.common.GameState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Persistable;

/**
 * A row of {@code games} as it is created (document 10, section 7.5). Only creation writes it through JPA; every later
 * change goes through the state recorder's compare-and-set updates (DB-05), so the moments and counts it never sets
 * aren't mapped here.
 */
@Entity
@Table(name = "games")
public class GameEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(name = "projector_key", length = 22)
    private @Nullable String projectorKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GameState state;

    @Column(name = "is_test", nullable = false)
    private boolean test;

    @Column(name = "run_plan_id")
    private @Nullable UUID runPlanId;

    @Column(name = "run_plan_name", nullable = false, length = 60)
    private String runPlanName;

    @Column(name = "round_length_minutes", nullable = false)
    private short roundLengthMinutes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String snapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** True until the row is saved or loaded, so a save inserts without first selecting (assigned keys). */
    @Transient
    private boolean isNew;

    protected GameEntity() {
        this(new UUID(0, 0), "", "", false, new UUID(0, 0), "", 3, "{}", Instant.EPOCH);
        this.isNew = false;
    }

    /** A new game in CREATED, with its projector key and its snapshot as JSON. */
    public GameEntity(
            UUID id,
            String code,
            String projectorKey,
            boolean test,
            UUID runPlanId,
            String runPlanName,
            int roundLengthMinutes,
            String snapshot,
            Instant now) {
        this.id = id;
        this.code = code;
        this.projectorKey = projectorKey;
        this.state = GameState.CREATED;
        this.test = test;
        this.runPlanId = runPlanId;
        this.runPlanName = runPlanName;
        this.roundLengthMinutes = (short) roundLengthMinutes;
        this.snapshot = snapshot;
        this.createdAt = now;
        this.updatedAt = now;
        this.isNew = true;
    }

    public UUID id() {
        return id;
    }

    public String code() {
        return code;
    }

    /** Present while the game is open, and cleared when it's closed or cancelled (DEC-109). */
    public @Nullable String projectorKey() {
        return projectorKey;
    }

    public GameState state() {
        return state;
    }

    public boolean test() {
        return test;
    }

    public String runPlanName() {
        return runPlanName;
    }

    public int roundLengthMinutes() {
        return roundLengthMinutes;
    }

    public Instant createdAt() {
        return createdAt;
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
