package app.deliveryhero.seed;

import app.deliveryhero.seed.SeedImporter.Imported;
import app.deliveryhero.seed.SeedImporter.Outcome;
import app.deliveryhero.seed.SeedImporter.Refused;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;

/** The one-off seed loader command: {@code docker compose run --rm backend seed <file>} (LLD section 5.10). */
@Component
public class SeedCommand {

    /** The first program argument that selects the seed loader instead of the web application. */
    public static final String NAME = "seed";

    /** Exit status: the file was imported. */
    public static final int IMPORTED = 0;

    /** Exit status: nothing was imported (LLD section 5.10 step 3). */
    public static final int FAILED = 1;

    private final SeedImporter importer;

    SeedCommand(SeedImporter importer) {
        this.importer = importer;
    }

    /**
     * Imports the seed file named in {@code args}, printing the result for the operator.
     *
     * @return the process exit status
     */
    public int run(List<String> args) {
        PrintStream out = System.out;
        if (args.size() != 1) {
            out.println("Usage: seed <file>");
            return FAILED;
        }
        Outcome outcome = importer.importFile(Path.of(args.getFirst()));
        outcome.warnings().forEach(warning -> out.println("WARNING " + warning));
        return switch (outcome) {
            case Imported imported -> {
                out.printf(
                        "Imported %d characters, %d tasks and %d run plans.%n",
                        imported.counts().characters(),
                        imported.counts().tasks(),
                        imported.counts().runPlans());
                yield IMPORTED;
            }
            case Refused refused -> {
                refused.errors().forEach(error -> out.println("ERROR " + error));
                out.printf(
                        "%d error(s). Nothing was imported.%n", refused.errors().size());
                yield FAILED;
            }
        };
    }
}
