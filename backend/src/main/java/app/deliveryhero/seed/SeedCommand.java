package app.deliveryhero.seed;

import java.util.List;
import org.springframework.stereotype.Component;

/** The one-off seed loader command: {@code docker compose run --rm backend seed <file>} (LLD section 5.10). */
@Component
public class SeedCommand {

    /** The first program argument that selects the seed loader instead of the web application. */
    public static final String NAME = "seed";

    /** Exit status while the loader doesn't exist yet. */
    static final int NOT_IMPLEMENTED = 2;

    /**
     * Imports the seed file named in {@code args}.
     *
     * @return the process exit status
     */
    public int run(List<String> args) {
        // TODO(US-56): validate and import the file with SeedImporter; refuse while a game is in progress
        System.out.println("The seed loader arrives with US-56. Nothing was imported.");
        return NOT_IMPLEMENTED;
    }
}
