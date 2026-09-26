package app.deliveryhero.lifecycle;

import app.deliveryhero.common.GameState;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<GameEntity, UUID> {

    /** The game in none of these states; with one open game at a time (DEC-101) there is at most one. */
    Optional<GameEntity> findFirstByStateNotIn(Collection<GameState> states);

    boolean existsByStateNotIn(Collection<GameState> states);

    boolean existsByCode(String code);
}
