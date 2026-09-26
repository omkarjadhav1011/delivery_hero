package app.deliveryhero.content;

import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.DeliveryHeroException;
import app.deliveryhero.common.Ids;
import app.deliveryhero.common.TaskType;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Creates, reads and changes tasks for the admin panel, validating every save (LLD section 5.3, FR-069). */
@Service
public class TaskService {

    private final TaskRepository tasks;
    private final RunPlanRepository plans;
    private final CharacterRepository characters;
    private final ContentValidator validator;
    private final JsonMapper json;
    private final Clock clock;
    private final SecureRandom random;

    TaskService(
            TaskRepository tasks,
            RunPlanRepository plans,
            CharacterRepository characters,
            ContentValidator validator,
            JsonMapper json,
            Clock clock,
            SecureRandom random) {
        this.tasks = tasks;
        this.plans = plans;
        this.characters = characters;
        this.validator = validator;
        this.json = json;
        this.clock = clock;
        this.random = random;
    }

    /** The library's rows that match the filter, by key, without answers (FR-070). */
    @Transactional(readOnly = true)
    public List<TaskSummary> list(TaskFilter filter) {
        Map<UUID, Long> uses = plans.countUses().stream()
                .collect(Collectors.toMap(RunPlanRepository.TaskUse::getTaskId, RunPlanRepository.TaskUse::getPlans));
        // Sorted here, so the order doesn't depend on the database's collation
        return tasks.findAll(filter.specification()).stream()
                .sorted(Comparator.comparing(TaskEntity::taskKey))
                .map(entity -> new TaskSummary(
                        entity.id(),
                        entity.taskKey(),
                        entity.role(),
                        entity.kind(),
                        entity.phase(),
                        entity.taskType(),
                        entity.prompt(),
                        TaskDefinition.effectiveTimeLimitSeconds(
                                entity.timeLimitSeconds(), entity.kind(), entity.taskType()),
                        uses.getOrDefault(entity.id(), 0L).intValue(),
                        entity.version()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskDetail get(UUID id) {
        return detail(find(id), List.of());
    }

    @Transactional
    public TaskDetail create(TaskInput input) {
        TaskDefinition task = definition(input, input.key(), false);
        if (tasks.existsByTaskKey(task.key())) {
            throw invalid(List.of(new Issue("key", "DUPLICATE_KEY", "The key " + task.key() + " is already used.")));
        }
        ValidationReport report = checked(task);
        Instant now = now();
        TaskEntity entity = new TaskEntity(Ids.newUuid(random), task.key(), now);
        entity.apply(task, codeJson(task), json.writeValueAsString(task.content()), now);
        try {
            return detail(tasks.saveAndFlush(entity), report.warnings());
        } catch (DataIntegrityViolationException sameKeyMeanwhile) {
            throw invalid(List.of(new Issue("key", "DUPLICATE_KEY", "The key " + task.key() + " is already used.")));
        }
    }

    /** The key can't change after creation, so the stored one is kept whatever the input says (API section 7.4). */
    @Transactional
    public TaskDetail update(UUID id, TaskInput input) {
        TaskEntity entity = find(id);
        TaskDefinition task = definition(input, entity.taskKey(), true);
        requireVersion(entity, input.version());
        ValidationReport report = checked(task);
        entity.apply(task, codeJson(task), json.writeValueAsString(task.content()), now());
        try {
            return detail(tasks.saveAndFlush(entity), report.warnings());
        } catch (OptimisticLockingFailureException changedMeanwhile) {
            throw new DeliveryHeroException(ApiErrorCode.EDIT_CONFLICT);
        }
    }

    /** What phones would get for the input, from the same mapping they use; nothing is saved (AP-05). */
    @Transactional(readOnly = true)
    public PublicTaskView publicView(TaskInput input) {
        TaskDefinition task = definition(input, input.key(), false);
        checked(task);
        String characterName = characters
                .findById(task.role())
                .map(CharacterEntity::displayName)
                .orElseThrow(() -> invalid(List.of(new Issue("role", "REQUIRED", "No character has this role yet."))));
        return PublicTaskView.of(task, characterName);
    }

    /** Deletes a task no run plan uses; otherwise TASK_IN_USE names each plan (FR-071). */
    @Transactional
    public void delete(UUID id, int version) {
        TaskEntity entity = find(id);
        requireVersion(entity, version);
        List<RunPlanEntity> users = plans.findUsing(entity.id());
        if (!users.isEmpty()) {
            List<String> names = users.stream().map(RunPlanEntity::name).toList();
            throw new DeliveryHeroException(
                    ApiErrorCode.TASK_IN_USE,
                    "This task is used by: " + String.join(", ", names),
                    names.stream()
                            .map(name -> new Issue("usedBy", "TASK_IN_USE", name))
                            .toList());
        }
        try {
            tasks.delete(entity);
            tasks.flush();
        } catch (DataIntegrityViolationException usedMeanwhile) {
            throw new DeliveryHeroException(ApiErrorCode.TASK_IN_USE);
        } catch (OptimisticLockingFailureException changedMeanwhile) {
            throw new DeliveryHeroException(ApiErrorCode.EDIT_CONFLICT);
        }
    }

    /** The time as PostgreSQL stores it, so a saved detail equals the one read back. */
    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    /** EDIT_CONFLICT unless the version sent is the stored one, so an outdated copy never overwrites (FR-073). */
    private static void requireVersion(TaskEntity entity, @Nullable Integer version) {
        if (version == null || version != entity.version()) {
            throw new DeliveryHeroException(ApiErrorCode.EDIT_CONFLICT);
        }
    }

    private TaskEntity find(UUID id) {
        return tasks.findById(id).orElseThrow(() -> new DeliveryHeroException(ApiErrorCode.NOT_FOUND));
    }

    private ValidationReport checked(TaskDefinition task) {
        ValidationReport report = validator.validateTask(task);
        if (report.hasErrors()) {
            throw invalid(report.errors());
        }
        return report;
    }

    /** The input as a definition, or VALIDATION_FAILED listing each missing field. */
    private TaskDefinition definition(TaskInput input, @Nullable String key, boolean versionRequired) {
        List<Issue> missing = new ArrayList<>();
        require(missing, "key", key);
        require(missing, "role", input.role());
        require(missing, "kind", input.kind());
        require(missing, "type", input.type());
        require(missing, "prompt", input.prompt());
        if (versionRequired) {
            require(missing, "version", input.version());
        }
        TaskType type = input.type();
        TaskContent content = type == null ? null : content(type, input.content());
        if (content == null) {
            missing.add(new Issue("content", "REQUIRED", "Fill in the content for the task type."));
        }
        if (key == null
                || input.role() == null
                || input.kind() == null
                || type == null
                || input.prompt() == null
                || content == null
                || !missing.isEmpty()) {
            throw invalid(missing);
        }
        return new TaskDefinition(
                key,
                input.role(),
                input.kind(),
                input.phase(),
                type,
                input.prompt(),
                input.code(),
                input.timeLimitSeconds(),
                content,
                input.explanation());
    }

    /** The content in the format for the type, or null when it's missing or doesn't fit (document 10, 8.3). */
    private @Nullable TaskContent content(TaskType type, @Nullable Map<String, Object> content) {
        if (content == null) {
            return null;
        }
        try {
            TaskContent parsed = json.readerFor(format(type))
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .with(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                    .with(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
                    .with(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                    .readValue(json.<JsonNode>valueToTree(content));
            return hasNullElement(parsed) ? null : parsed;
        } catch (JacksonException unfit) {
            return null;
        }
    }

    private static Class<? extends TaskContent> format(TaskType type) {
        return switch (type) {
            case MULTIPLE_CHOICE -> MultipleChoiceContent.class;
            case YES_NO -> YesNoContent.class;
            case ORDER -> OrderContent.class;
            case PROBLEM_WORDS -> ProblemWordsContent.class;
        };
    }

    private static boolean hasNullElement(TaskContent content) {
        return switch (content) {
            case MultipleChoiceContent c -> c.options().contains(null);
            case OrderContent c -> c.items().contains(null);
            case YesNoContent c -> false;
            case ProblemWordsContent c -> false;
        };
    }

    private @Nullable String codeJson(TaskDefinition task) {
        CodeSnippet code = task.code();
        return code == null ? null : json.writeValueAsString(code);
    }

    /** The stored task as a definition, read back from its JSON columns. */
    TaskDefinition definition(TaskEntity entity) {
        String code = entity.code();
        return new TaskDefinition(
                entity.taskKey(),
                entity.role(),
                entity.kind(),
                entity.phase(),
                entity.taskType(),
                entity.prompt(),
                code == null ? null : json.readValue(code, CodeSnippet.class),
                entity.timeLimitSeconds(),
                json.readValue(entity.content(), format(entity.taskType())),
                entity.explanation());
    }

    private TaskDetail detail(TaskEntity entity, List<Issue> warnings) {
        TaskDefinition task = definition(entity);
        List<TaskDetail.PlanReference> usedBy = plans.findUsing(entity.id()).stream()
                .map(plan -> new TaskDetail.PlanReference(plan.id(), plan.name()))
                .toList();
        return new TaskDetail(
                entity.id(),
                task.key(),
                task.role(),
                task.kind(),
                task.phase(),
                task.type(),
                task.prompt(),
                task.code(),
                task.timeLimitSeconds(),
                task.effectiveTimeLimitSeconds(),
                task.content(),
                task.explanation(),
                entity.version(),
                usedBy,
                entity.createdAt(),
                entity.updatedAt(),
                warnings);
    }

    private static void require(List<Issue> missing, String path, @Nullable Object value) {
        if (value == null) {
            missing.add(new Issue(path, "REQUIRED", "This field is required."));
        }
    }

    private static DeliveryHeroException invalid(List<Issue> errors) {
        return new DeliveryHeroException(ApiErrorCode.VALIDATION_FAILED, null, errors);
    }
}
