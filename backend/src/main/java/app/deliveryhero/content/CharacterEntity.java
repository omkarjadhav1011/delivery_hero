package app.deliveryhero.content;

import app.deliveryhero.common.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A row of {@code characters}, keyed by role (document 10, section 7.1). Reaction lines are JSON arrays. */
@Entity
@Table(name = "characters")
public class CharacterEntity {

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

    protected CharacterEntity() {
        this(Role.MANAGER);
    }

    public CharacterEntity(Role role) {
        this.role = role;
        this.displayName = "";
        this.introLine = "";
        this.correctLines = "[]";
        this.wrongLines = "[]";
        this.updatedAt = Instant.EPOCH;
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
}
