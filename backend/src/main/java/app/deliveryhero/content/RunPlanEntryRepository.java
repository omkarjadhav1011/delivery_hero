package app.deliveryhero.content;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RunPlanEntryRepository extends JpaRepository<RunPlanEntryEntity, RunPlanEntryEntity.Id> {

    /** A plan's entries, each list in play order. */
    @Query("SELECT e FROM RunPlanEntryEntity e WHERE e.id.runPlanId = :runPlanId ORDER BY e.listName, e.sortOrder")
    List<RunPlanEntryEntity> findByRunPlanId(@Param("runPlanId") UUID runPlanId);

    /** Removes a plan's entries in one statement, so entries with the same keys can follow in the same transaction. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM RunPlanEntryEntity e WHERE e.id.runPlanId = :runPlanId")
    int deleteByRunPlanId(@Param("runPlanId") UUID runPlanId);
}
