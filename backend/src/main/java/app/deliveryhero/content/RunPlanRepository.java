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

    /** How many plans use each task, in a list or as the incident, for the task library in one query (FR-070). */
    @Query(value = """
            SELECT task_id AS taskId, count(DISTINCT run_plan_id) AS plans FROM (
                SELECT task_id, run_plan_id FROM run_plan_entries
                UNION ALL
                SELECT incident_task_id, id FROM run_plans WHERE incident_task_id IS NOT NULL
            ) uses
            GROUP BY task_id""", nativeQuery = true)
    List<TaskUse> countUses();

    /** A task and the number of plans that use it. */
    interface TaskUse {
        UUID getTaskId();

        long getPlans();
    }
}
