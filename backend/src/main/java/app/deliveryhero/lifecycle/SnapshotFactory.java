package app.deliveryhero.lifecycle;

import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.DeliveryHeroException;
import app.deliveryhero.common.Phase;
import app.deliveryhero.common.Role;
import app.deliveryhero.content.CharacterDefinition;
import app.deliveryhero.content.CharacterService;
import app.deliveryhero.content.GameSnapshot;
import app.deliveryhero.content.RunPlanContents;
import app.deliveryhero.content.RunPlanService;
import app.deliveryhero.content.TaskDefinition;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Copies a run plan, its tasks and every character into a {@link GameSnapshot}, with each time limit resolved to
 * milliseconds (LLD section 5.8; document 10, section 8.5).
 */
@Component
public class SnapshotFactory {

    private final RunPlanService plans;
    private final CharacterService characters;

    SnapshotFactory(RunPlanService plans, CharacterService characters) {
        this.plans = plans;
        this.characters = characters;
    }

    /** The snapshot of the plan as it is stored now; 404 when there's no such plan. */
    public GameSnapshot create(UUID runPlanId) {
        return create(plans.load(runPlanId).orElseThrow(() -> new DeliveryHeroException(ApiErrorCode.NOT_FOUND)));
    }

    GameSnapshot create(RunPlanContents plan) {
        Map<Role, GameSnapshot.Character> cast = new EnumMap<>(Role.class);
        for (CharacterDefinition character : characters.all()) {
            cast.put(
                    character.role(),
                    new GameSnapshot.Character(
                            character.displayName(),
                            character.introLine(),
                            character.correctLines(),
                            character.wrongLines()));
        }
        Map<Phase, List<GameSnapshot.Task>> phases = new EnumMap<>(Phase.class);
        plan.phases().forEach((phase, tasks) -> phases.put(phase, tasks(tasks)));
        TaskDefinition incident = plan.incident();
        return new GameSnapshot(
                GameSnapshot.FORMAT_VERSION,
                plan.name(),
                plan.roundLengthMinutes() * 60,
                cast,
                tasks(plan.practice()),
                incident == null ? null : task(incident),
                phases);
    }

    private static List<GameSnapshot.Task> tasks(List<TaskDefinition> tasks) {
        return tasks.stream().map(SnapshotFactory::task).toList();
    }

    private static GameSnapshot.Task task(TaskDefinition task) {
        return new GameSnapshot.Task(
                task.key(),
                task.role(),
                task.kind(),
                task.type(),
                task.prompt(),
                task.code(),
                task.effectiveTimeLimitSeconds() * 1000,
                task.content(),
                task.explanation());
    }
}
