package app.deliveryhero.content;

import app.deliveryhero.common.Phase;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads run plans with their tasks (LLD section 5.3). The run plan editor's writes come with US-57 (S2-09). */
@Service
public class RunPlanService {

    private final RunPlanRepository plans;
    private final RunPlanEntryRepository entries;
    private final TaskRepository tasks;
    private final TaskService taskService;
    private final ContentValidator validator;

    RunPlanService(
            RunPlanRepository plans,
            RunPlanEntryRepository entries,
            TaskRepository tasks,
            TaskService taskService,
            ContentValidator validator) {
        this.plans = plans;
        this.entries = entries;
        this.tasks = tasks;
        this.taskService = taskService;
        this.validator = validator;
    }

    /** The plan with its lists as they are stored now, or empty when there's no such plan. */
    @Transactional(readOnly = true)
    public Optional<RunPlanContents> load(UUID id) {
        return plans.findById(id).map(this::contents);
    }

    /** Every plan by name, with the counts the plan picker shows (API section 7.6). */
    @Transactional(readOnly = true)
    public List<RunPlanSummary> summaries() {
        return plans.findAll(Sort.by("name")).stream()
                .map(this::contents)
                .map(plan -> new RunPlanSummary(
                        plan.id(),
                        plan.key(),
                        plan.name(),
                        plan.roundLengthMinutes(),
                        plan.phases().values().stream().mapToInt(List::size).sum(),
                        validator.validateForGame(plan).errors().size(),
                        0, // TODO(US-58): the readiness warnings of BR-13
                        plan.version()))
                .toList();
    }

    private RunPlanContents contents(RunPlanEntity plan) {
        List<RunPlanEntryEntity> rows = entries.findByRunPlanId(plan.id());
        List<UUID> ids =
                new ArrayList<>(rows.stream().map(RunPlanEntryEntity::taskId).toList());
        UUID incidentId = plan.incidentTaskId();
        if (incidentId != null) {
            ids.add(incidentId);
        }
        // Entries reference tasks by foreign key, so every ID is found
        Map<UUID, TaskDefinition> byId = tasks.findAllById(ids).stream()
                .collect(Collectors.toMap(TaskEntity::id, taskService::definition, (a, b) -> a));
        Function<RunPlanList, List<TaskDefinition>> list = name -> rows.stream()
                .filter(row -> row.listName() == name)
                .map(row -> byId.get(row.taskId()))
                .toList();
        Map<Phase, List<TaskDefinition>> phases = new EnumMap<>(Phase.class);
        for (Phase phase : Phase.values()) {
            phases.put(phase, list.apply(RunPlanList.valueOf(phase.name())));
        }
        return new RunPlanContents(
                plan.id(),
                plan.planKey(),
                plan.name(),
                plan.roundLengthMinutes(),
                plan.version(),
                list.apply(RunPlanList.PRACTICE),
                incidentId == null ? null : byId.get(incidentId),
                phases);
    }
}
