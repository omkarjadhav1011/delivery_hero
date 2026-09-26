package app.deliveryhero.content;

import app.deliveryhero.common.Role;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

/** A row of {@code characters}, keyed by role (document 10, section 7.1). Reaction lines are JSON arrays. */
@Entity
@Table(name = "characters")
public class CharacterEntity implements Persistable<Role> {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Role role;

    @Column(name = "display_name", nullable = false, length = 20)
    private String displayName;

    @Column(name = "intro_line", nullable = false, length = 80)
    private String introLine;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "correct_lines", nullable = false)
    private String correctLines;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "wrong_lines", nullable = false)
    private String wrongLines;

    @Version
    private int version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** True until the row is saved or loaded, so a save inserts without first selecting (assigned keys). */
    @Transient
    private boolean isNew;

    protected CharacterEntity() {
        this(Role.MANAGER);
        this.isNew = false;
    }

    public CharacterEntity(Role role) {
        this.role = role;
        this.displayName = "";
        this.introLine = "";
        this.correctLines = "[]";
        this.wrongLines = "[]";
        this.updatedAt = Instant.EPOCH;
        this.isNew = true;
    }

    /** Copies every field but the role; the reaction lines arrive already as JSON arrays. */
    public void apply(CharacterDefinition character, String correctLinesJson, String wrongLinesJson, Instant now) {
        this.displayName = character.displayName();
        this.introLine = character.introLine();
        this.correctLines = correctLinesJson;
        this.wrongLines = wrongLinesJson;
        this.updatedAt = now;
    }

    public Role role() {
        return role;
    }

    public String displayName() {
        return displayName;
    }

    public String introLine() {
        return introLine;
    }

    /** The JSON array of the three correct-answer lines. */
    public String correctLines() {
        return correctLines;
    }

    /** The JSON array of the three wrong-answer lines. */
    public String wrongLines() {
        return wrongLines;
    }

    public int version() {
        return version;
    }

    @Override
    public Role getId() {
        return role;
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
