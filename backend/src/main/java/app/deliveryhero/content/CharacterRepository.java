package app.deliveryhero.content;

import app.deliveryhero.common.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterRepository extends JpaRepository<CharacterEntity, Role> {}
