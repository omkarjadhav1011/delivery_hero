package app.deliveryhero.content;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunPlanRepository extends JpaRepository<RunPlanEntity, UUID> {

    Optional<RunPlanEntity> findByPlanKey(String planKey);
}
