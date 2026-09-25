package app.deliveryhero.seed;

import app.deliveryhero.common.Phase;
import app.deliveryhero.common.Role;
import app.deliveryhero.common.TaskKind;
import app.deliveryhero.common.TaskType;
import app.deliveryhero.content.CharacterDefinition;
import app.deliveryhero.content.CodeSnippet;
import app.deliveryhero.content.ContentValidator;
import app.deliveryhero.content.Issue;
import app.deliveryhero.content.MultipleChoiceContent;
import app.deliveryhero.content.OrderContent;
import app.deliveryhero.content.ProblemWordsContent;
import app.deliveryhero.content.RunPlanDefinition;
import app.deliveryhero.content.TaskContent;
import app.deliveryhero.content.TaskDefinition;
import app.deliveryhero.content.TaskRepository;
import app.deliveryhero.content.ValidationReport;
import app.deliveryhero.content.YesNoContent;
import app.deliveryhero.seed.SeedWriter.Counts;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Parses the seed file, validates all of it, and only then hands it to {@link SeedWriter} (LLD section 5.10 steps 3
 * and 4, FR-075). Reported problems name the item by its key and the field by its path; they never quote task text.
 */
@Component
public class SeedImporter {

    /** What happened to a seed file. */
    public sealed interface Outcome permits Imported, Refused {
        List<String> warnings();
    }

    public record Imported(Counts counts, List<String> warnings) implements Outcome {}

    public record Refused(List<String> errors, List<String> warnings) implements Outcome {}

    private static final Logger log = LoggerFactory.getLogger(SeedImporter.class);

    private final JsonMapper json;
    private final ContentValidator validator;
    private final TaskRepository storedTasks;
    private final SeedWriter writer;

    SeedImporter(JsonMapper json, ContentValidator validator, TaskRepository storedTasks, SeedWriter writer) {
        this.json = json;
        this.validator = validator;
        this.storedTasks = storedTasks;
        this.writer = writer;
    }

    public Outcome importFile(Path file) {
        SeedFile seed;
        try (InputStream in = Files.newInputStream(file)) {
            seed = json.readValue(in, SeedFile.class);
        } catch (IOException e) {
            return new Refused(
                    List.of("file " + file + ": can't be read (" + e.getClass().getSimpleName() + ")"), List.of());
        } catch (JacksonException e) {
            // Only the place: Jackson's message can quote the file's text
            return new Refused(
                    List.of("file " + file + ": isn't a valid seed file ("
                            + e.getClass().getSimpleName() + " at line "
                            + e.getLocation().getLineNr() + ", column "
                            + e.getLocation().getColumnNr() + ")"),
                    List.of());
        }
        Check check = new Check();
        check.run(seed);
        if (!check.errors.isEmpty()) {
            return new Refused(check.errors, check.warnings);
        }
        Counts counts = writer.write(check.characters, check.tasks, check.plans);
        log.atInfo()
                .addKeyValue("event", "SEED_IMPORTED")
                .addKeyValue("characters", counts.characters())
                .addKeyValue("tasks", counts.tasks())
                .addKeyValue("runPlans", counts.runPlans())
                .log("Seed imported");
        return new Imported(counts, check.warnings);
    }

    /** One pass over the file, collecting definitions, errors and warnings. */
    private final class Check {

        final List<String> errors = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        final List<CharacterDefinition> characters = new ArrayList<>();
        final List<TaskDefinition> tasks = new ArrayList<>();
        final List<RunPlanDefinition> plans = new ArrayList<>();

        void run(SeedFile seed) {
            if (!Objects.equals(seed.formatVersion(), 1)) {
                errors.add("file: formatVersion must be 1");
            }
            checkCharacters(orEmpty(seed.characters()));
            Set<String> taskKeys = new HashSet<>();
            List<SeedFile.Task> seedTasks = orEmpty(seed.tasks());
            for (int i = 0; i < seedTasks.size(); i++) {
                SeedFile.Task raw = seedTasks.get(i);
                if (raw == null) {
                    errors.add("tasks[" + i + "]: is empty");
                    continue;
                }
                String where = "task " + label(raw.key(), "tasks[" + i + "]");
                if (raw.key() != null && !taskKeys.add(raw.key())) {
                    errors.add(where + ": key: the key is used more than once");
                }
                TaskDefinition task = toTask(raw, where);
                if (task != null) {
                    report(where, validator.validateSeedTask(task));
                    tasks.add(task);
                }
            }
            Set<String> planKeys = new HashSet<>();
            List<SeedFile.RunPlan> seedPlans = orEmpty(seed.runPlans());
            for (int i = 0; i < seedPlans.size(); i++) {
                SeedFile.RunPlan raw = seedPlans.get(i);
                if (raw == null) {
                    errors.add("runPlans[" + i + "]: is empty");
                    continue;
                }
                String where = "run plan " + label(raw.key(), "runPlans[" + i + "]");
                if (raw.key() != null && !planKeys.add(raw.key())) {
                    errors.add(where + ": key: the key is used more than once");
                }
                RunPlanDefinition plan = toPlan(raw, where);
                if (plan != null) {
                    report(where, validator.validateRunPlan(plan));
                    report(
                            where,
                            validator.validateRunPlanKeys(
                                    plan, key -> taskKeys.contains(key) || storedTasks.existsByTaskKey(key)));
                    plans.add(plan);
                }
            }
        }

        private void checkCharacters(List<SeedFile.Character> seedCharacters) {
            Set<Role> roles = new HashSet<>();
            for (int i = 0; i < seedCharacters.size(); i++) {
                SeedFile.Character raw = seedCharacters.get(i);
                if (raw == null) {
                    errors.add("characters[" + i + "]: is empty");
                    continue;
                }
                String where = "character " + label(raw.role(), "characters[" + i + "]");
                Role role = parse(Role.class, raw.role(), where, "role");
                if (role == null) {
                    continue;
                }
                if (!roles.add(role)) {
                    errors.add(where + ": role: the role is used more than once");
                }
                CharacterDefinition character = new CharacterDefinition(
                        role,
                        text(raw.displayName()),
                        text(raw.introLine()),
                        texts(raw.correctLines()),
                        texts(raw.wrongLines()));
                report(where, validator.validateCharacter(character));
                characters.add(character);
            }
        }

        private @Nullable TaskDefinition toTask(SeedFile.Task raw, String where) {
            Role role = parse(Role.class, raw.role(), where, "role");
            TaskKind kind = parse(TaskKind.class, raw.kind(), where, "kind");
            TaskType type = parse(TaskType.class, raw.type(), where, "type");
            Phase phase = raw.phase() == null ? null : parse(Phase.class, raw.phase(), where, "phase");
            Integer timeLimit = wholeNumber(raw.timeLimitSeconds(), where, "timeLimitSeconds");
            if (role == null || kind == null || type == null || (raw.phase() != null && phase == null)) {
                return null;
            }
            SeedFile.Code code = raw.code();
            return new TaskDefinition(
                    text(raw.key()),
                    role,
                    kind,
                    phase,
                    type,
                    text(raw.prompt()),
                    code == null ? null : new CodeSnippet(text(code.language()), text(code.text())),
                    timeLimit,
                    content(raw, type, where),
                    raw.explanation());
        }

        private TaskContent content(SeedFile.Task raw, TaskType type, String where) {
            return switch (type) {
                case MULTIPLE_CHOICE ->
                    new MultipleChoiceContent(present(raw.options(), where, "options").stream()
                            .map(o ->
                                    new MultipleChoiceContent.Option(text(o.text()), Boolean.TRUE.equals(o.correct())))
                            .toList());
                case YES_NO -> {
                    if (!"YES".equals(raw.answer()) && !"NO".equals(raw.answer())) {
                        errors.add(where + ": answer: must be YES or NO");
                    }
                    yield new YesNoContent("YES".equals(raw.answer()));
                }
                case ORDER ->
                    new OrderContent(present(raw.items(), where, "items").stream()
                            .map(item -> new OrderContent.Item(
                                    text(item.text()), Objects.requireNonNullElse(item.correctPosition(), 0)))
                            .toList());
                case PROBLEM_WORDS -> new ProblemWordsContent(text(raw.text()), Boolean.TRUE.equals(raw.monospace()));
            };
        }

        private @Nullable RunPlanDefinition toPlan(SeedFile.RunPlan raw, String where) {
            Integer minutes = wholeNumber(raw.roundLengthMinutes(), where, "roundLengthMinutes");
            if (raw.roundLengthMinutes() == null) {
                errors.add(where + ": roundLengthMinutes: is required");
            }
            Map<Phase, List<String>> phases = new EnumMap<>(Phase.class);
            boolean phasesValid = true;
            for (Map.Entry<String, List<String>> entry : Objects.requireNonNullElse(
                            raw.phases(), Map.<String, List<String>>of())
                    .entrySet()) {
                Phase phase = parse(Phase.class, entry.getKey(), where, "phases");
                if (phase == null) {
                    phasesValid = false;
                } else {
                    phases.put(phase, texts(entry.getValue()));
                }
            }
            if (minutes == null || !phasesValid) {
                return null;
            }
            return new RunPlanDefinition(
                    text(raw.key()), text(raw.name()), minutes, texts(raw.practice()), raw.incident(), phases);
        }

        private <E extends Enum<E>> @Nullable E parse(
                Class<E> type, @Nullable String value, String where, String field) {
            for (E constant : type.getEnumConstants()) {
                if (constant.name().equals(value)) {
                    return constant;
                }
            }
            errors.add(where + ": " + field + ": must be one of " + Arrays.toString(type.getEnumConstants()));
            return null;
        }

        private @Nullable Integer wholeNumber(@Nullable BigDecimal value, String where, String field) {
            if (value == null) {
                return null;
            }
            try {
                return value.stripTrailingZeros().intValueExact();
            } catch (ArithmeticException e) {
                errors.add(where + ": " + field + ": must be a whole number");
                return null;
            }
        }

        private void report(String where, ValidationReport report) {
            report.errors().forEach(issue -> errors.add(line(where, issue)));
            report.warnings().forEach(issue -> warnings.add(line(where, issue)));
        }

        private static String line(String where, Issue issue) {
            return where + ": " + issue.path() + ": " + issue.message();
        }

        private static String label(@Nullable String key, String fallback) {
            return key == null || key.isBlank() ? fallback : key;
        }

        private static String text(@Nullable String value) {
            return value == null ? "" : value;
        }

        /** The list without its null entries, each reported by its index. */
        private <T> List<T> present(@Nullable List<@Nullable T> list, String where, String field) {
            List<T> entries = new ArrayList<>();
            List<@Nullable T> all = list == null ? List.of() : list;
            for (int i = 0; i < all.size(); i++) {
                T entry = all.get(i);
                if (entry == null) {
                    errors.add(where + ": " + field + "[" + i + "]: is empty");
                } else {
                    entries.add(entry);
                }
            }
            return entries;
        }

        /** Text entries with a missing one as empty text, so the length rules report it. */
        private static List<String> texts(@Nullable List<@Nullable String> list) {
            return list == null ? List.of() : list.stream().map(Check::text).toList();
        }

        private static <T> List<T> orEmpty(@Nullable List<T> list) {
            return list == null ? List.of() : list;
        }
    }
}
