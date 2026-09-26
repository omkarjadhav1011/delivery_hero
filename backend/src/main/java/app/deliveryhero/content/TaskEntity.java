package app.deliveryhero.content;

import app.deliveryhero.common.Phase;
import app.deliveryhero.common.Role;
import app.deliveryhero.common.TaskKind;
import app.deliveryhero.common.TaskType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Persistable;

/** A row of {@code tasks} (document 10, section 7.2). Code and content are JSON, shaped by the task type. */
@Entity
@Table(name = "tasks")
public class TaskEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "task_key", nullable = false, unique = true, length = 40)
    private String taskKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskKind kind;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private @Nullable Phase phase;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 16)
    private TaskType taskType;

    @Column(nullable = false, length = 200)
    private String prompt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "code")
    private @Nullable String code;

    @Column(name = "time_limit_seconds")
    private @Nullable Short timeLimitSeconds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String content;

    @Column(length = 300)
    private @Nullable String explanation;

    @Version
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** True until the row is saved or loaded, so a save inserts without first selecting (assigned keys). */
    @Transient
    private boolean isNew;

    protected TaskEntity() {
        this(new UUID(0, 0), "", Instant.EPOCH);
        this.isNew = false;
    }

    public TaskEntity(UUID id, String taskKey, Instant now) {
        this.id = id;
        this.taskKey = taskKey;
        this.role = Role.MANAGER;
        this.kind = TaskKind.SCORED;
        this.taskType = TaskType.MULTIPLE_CHOICE;
        this.prompt = "";
        this.content = "{}";
        this.createdAt = now;
        this.updatedAt = now;
        this.isNew = true;
    }

    /** Copies every field but the key; code and content arrive already as JSON. */
    public void apply(TaskDefinition task, @Nullable String codeJson, String contentJson, Instant now) {
        Integer limit = task.timeLimitSeconds();
        this.role = task.role();
        this.kind = task.kind();
        this.phase = task.phase();
        this.taskType = task.type();
        this.prompt = task.prompt();
        this.code = codeJson;
        this.timeLimitSeconds = limit == null ? null : limit.shortValue();
        this.content = contentJson;
        this.explanation = task.explanation();
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public String taskKey() {
        return taskKey;
    }

    public Role role() {
        return role;
    }

    public TaskKind kind() {
        return kind;
    }

    public @Nullable Phase phase() {
        return phase;
    }

    public TaskType taskType() {
        return taskType;
    }

    public String prompt() {
        return prompt;
    }

    public @Nullable String code() {
        return code;
    }

    public @Nullable Integer timeLimitSeconds() {
        return timeLimitSeconds == null ? null : timeLimitSeconds.intValue();
    }

    public String content() {
        return content;
    }

    public @Nullable String explanation() {
        return explanation;
    }

    public int version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
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
