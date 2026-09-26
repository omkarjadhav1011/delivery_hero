package app.deliveryhero.content;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RunPlanRepository extends JpaRepository<RunPlanEntity, UUID> {

    Optional<RunPlanEntity> findByPlanKey(String planKey);

    /** The plans that list a task or use it as their incident, by name. */
    @Query("""
            SELECT p FROM RunPlanEntity p
            WHERE p.incidentTaskId = :taskId
               OR p.id IN (SELECT e.id.runPlanId FROM RunPlanEntryEntity e WHERE e.id.taskId = :taskId)
            ORDER BY p.name""")
    List<RunPlanEntity> findUsing(@Param("taskId") UUID taskId);
}
