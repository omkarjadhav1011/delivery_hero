package app.deliveryhero.content;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.DeliveryHeroException;
import app.deliveryhero.common.Phase;
import app.deliveryhero.common.Role;
import app.deliveryhero.common.TaskKind;
import app.deliveryhero.common.TaskType;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import tools.jackson.databind.json.JsonMapper;

/**
 * A change saved by another admin between the read and the write passes the version compare, and is then caught by
 * the {@code @Version} column at the flush: that race is answered with EDIT_CONFLICT too (FR-073).
 */
class ConcurrentEditTest {

    private static final Instant NOW = Instant.parse("2026-09-30T10:12:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID ID = UUID.fromString("0c9e7a44-2f5b-4d1e-8a3f-6b7c8d9e0f12");

    private final JsonMapper json = JsonMapper.builder().build();
    private final TaskRepository tasks = mock(TaskRepository.class);
    private final RunPlanRepository plans = mock(RunPlanRepository.class);
    private final CharacterRepository characters = mock(CharacterRepository.class);
    private final TaskService taskService = new TaskService(
            tasks, plans, characters, new ContentValidator(), json, CLOCK, new SecureRandom(new byte[] {1}));
    private final CharacterService characterService =
            new CharacterService(characters, new ContentValidator(), json, CLOCK);

    @Test
    @DisplayName("AC-US53-01 race: a task changed between the read and the save is refused with EDIT_CONFLICT")
    void taskUpdateRace() {
        when(tasks.findById(ID)).thenReturn(Optional.of(new TaskEntity(ID, "tst-test-99", NOW)));
        when(tasks.saveAndFlush(any())).thenThrow(changedMeanwhile(TaskEntity.class));
        TaskInput input = new TaskInput(
                "tst-test-99",
                Role.TESTER,
                TaskKind.SCORED,
                Phase.TESTING,
                TaskType.YES_NO,
                "Ship on Friday?",
                null,
                null,
                Map.of("answerYes", false),
                "Not on a Friday.",
                0);

        assertThatThrownBy(() -> taskService.update(ID, input)).satisfies(ConcurrentEditTest::isEditConflict);
    }

    @Test
    @DisplayName("AC-US53-01 race: a task changed between the read and the delete is refused with EDIT_CONFLICT")
    void taskDeleteRace() {
        when(tasks.findById(ID)).thenReturn(Optional.of(new TaskEntity(ID, "tst-test-99", NOW)));
        when(plans.findUsing(ID)).thenReturn(List.of());
        doThrow(changedMeanwhile(TaskEntity.class)).when(tasks).flush();

        assertThatThrownBy(() -> taskService.delete(ID, 0)).satisfies(ConcurrentEditTest::isEditConflict);
    }

    @Test
    @DisplayName("AC-US53-02 race: a character changed between the read and the save is refused with EDIT_CONFLICT")
    void characterUpdateRace() {
        when(characters.findById(Role.TESTER)).thenReturn(Optional.of(new CharacterEntity(Role.TESTER)));
        when(characters.saveAndFlush(any())).thenThrow(changedMeanwhile(CharacterEntity.class));
        CharacterInput input = new CharacterInput(
                "Tessa",
                "Found another one!",
                List.of("Bug squashed!", "Test passed.", "Zero defects."),
                List.of("That bug reached production.", "Reopening the ticket.", "Severity: ouch."),
                0);

        assertThatThrownBy(() -> characterService.update(Role.TESTER, input))
                .satisfies(ConcurrentEditTest::isEditConflict);
    }

    private static ObjectOptimisticLockingFailureException changedMeanwhile(Class<?> entity) {
        return new ObjectOptimisticLockingFailureException(entity, ID);
    }

    private static void isEditConflict(Throwable thrown) {
        org.assertj.core.api.Assertions.assertThat(thrown)
                .isInstanceOfSatisfying(
                        DeliveryHeroException.class,
                        refused -> org.assertj.core.api.Assertions.assertThat(refused.code())
                                .isEqualTo(ApiErrorCode.EDIT_CONFLICT));
    }
}
