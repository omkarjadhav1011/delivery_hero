package app.deliveryhero.seed;

import app.deliveryhero.common.Phase;
import app.deliveryhero.content.CharacterDefinition;
import app.deliveryhero.content.CharacterEntity;
import app.deliveryhero.content.CharacterRepository;
import app.deliveryhero.content.CodeSnippet;
import app.deliveryhero.content.RunPlanDefinition;
import app.deliveryhero.content.RunPlanEntity;
import app.deliveryhero.content.RunPlanEntryEntity;
import app.deliveryhero.content.RunPlanEntryRepository;
import app.deliveryhero.content.RunPlanList;
import app.deliveryhero.content.RunPlanRepository;
import app.deliveryhero.content.TaskDefinition;
import app.deliveryhero.content.TaskEntity;
import app.deliveryhero.content.TaskRepository;
import app.deliveryhero.lifecycle.GameInProgressCheck;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes a validated seed in one transaction: characters by role, tasks by key and run plans by key with their entries
 * replaced (LLD section 5.10 step 4, document 10 section 8.4).
 */
@Component
public class SeedWriter {

    /** How many of each were written. */
    public record Counts(int characters, int tasks, int runPlans) {}

    /** A game opened after the command's first check; the transaction rolls back and nothing is written. */
    public static final class GameInProgressException extends RuntimeException {
        GameInProgressException() {
            super("A game is in progress");
        }
    }

    private final CharacterRepository characters;
    private final TaskRepository tasks;
    private final RunPlanRepository runPlans;
    private final RunPlanEntryRepository entries;
    private final JsonMapper json;
    private final Clock clock;
    private final GameInProgressCheck games;

    SeedWriter(
            CharacterRepository characters,
            TaskRepository tasks,
            RunPlanRepository runPlans,
            RunPlanEntryRepository entries,
            JsonMapper json,
            Clock clock,
            GameInProgressCheck games) {
        this.characters = characters;
        this.tasks = tasks;
        this.runPlans = runPlans;
        this.entries = entries;
        this.json = json;
        this.clock = clock;
        this.games = games;
    }

    @Transactional
    public Counts write(
            List<CharacterDefinition> seedCharacters,
            List<TaskDefinition> seedTasks,
            List<RunPlanDefinition> seedPlans) {
        if (games.anyGameInProgress()) {
            throw new GameInProgressException();
        }
        Instant now = clock.instant();
        for (CharacterDefinition character : seedCharacters) {
            CharacterEntity entity =
                    characters.findById(character.role()).orElseGet(() -> new CharacterEntity(character.role()));
            entity.apply(
                    character,
                    json.writeValueAsString(character.correctLines()),
                    json.writeValueAsString(character.wrongLines()),
                    now);
            characters.save(entity);
        }
        Map<String, UUID> taskIds = new HashMap<>();
        for (TaskDefinition task : seedTasks) {
            TaskEntity entity =
                    tasks.findByTaskKey(task.key()).orElseGet(() -> new TaskEntity(UUID.randomUUID(), task.key(), now));
            CodeSnippet code = task.code();
            entity.apply(
                    task,
                    code == null ? null : json.writeValueAsString(code),
                    json.writeValueAsString(task.content()),
                    now);
            taskIds.put(task.key(), tasks.save(entity).id());
        }
        for (RunPlanDefinition plan : seedPlans) {
            RunPlanEntity entity = runPlans.findByPlanKey(plan.key())
                    .orElseGet(() -> new RunPlanEntity(UUID.randomUUID(), plan.key(), now));
            String incident = plan.incident();
            entity.apply(plan, incident == null ? null : taskId(incident, taskIds), now);
            UUID planId = runPlans.save(entity).id();
            entries.deleteByRunPlanId(planId);
            entries.saveAll(entries(planId, plan, taskIds));
        }
        return new Counts(seedCharacters.size(), seedTasks.size(), seedPlans.size());
    }

    private List<RunPlanEntryEntity> entries(UUID planId, RunPlanDefinition plan, Map<String, UUID> taskIds) {
        List<RunPlanEntryEntity> rows = new ArrayList<>();
        addList(rows, planId, RunPlanList.PRACTICE, plan.practice(), taskIds);
        for (Phase phase : Phase.values()) {
            addList(
                    rows,
                    planId,
                    RunPlanList.valueOf(phase.name()),
                    plan.phases().getOrDefault(phase, List.of()),
                    taskIds);
        }
        return rows;
    }

    private void addList(
            List<RunPlanEntryEntity> rows,
            UUID planId,
            RunPlanList list,
            List<String> keys,
            Map<String, UUID> taskIds) {
        for (int i = 0; i < keys.size(); i++) {
            rows.add(new RunPlanEntryEntity(planId, taskId(keys.get(i), taskIds), list, i));
        }
    }

    /** A task named by a plan is in the file or, as the importer checked, already in the database. */
    private UUID taskId(String key, Map<String, UUID> taskIds) {
        @Nullable UUID id = taskIds.get(key);
        if (id != null) {
            return id;
        }
        UUID stored = tasks.findByTaskKey(key)
                .map(TaskEntity::id)
                .orElseThrow(() -> new IllegalStateException("The task " + key + " was deleted during the import"));
        taskIds.put(key, stored);
        return stored;
    }
}
