package tessera.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves where Tessera keeps its writable data. The original game wrote to a
 * relative {@code ./Resources/} path, which only worked when the program was
 * launched from the project root and broke entirely from a packaged jar. This
 * puts the leaderboard and settings under the user's home directory, which is
 * writable regardless of where the jar lives.
 */
public final class DataPaths {

    private static final String DIR_NAME = ".tessera";

    private DataPaths() {
    }

    /** The Tessera data directory, e.g. {@code ~/.tessera}. */
    public static Path dataDir() {
        String home = System.getProperty("user.home", ".");
        return Paths.get(home, DIR_NAME);
    }

    /** Create the data directory if it does not already exist. */
    public static void ensureDataDir() throws IOException {
        Path dir = dataDir();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }
}
