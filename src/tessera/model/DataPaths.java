package tessera.model;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

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

    /**
     * Write {@code lines} to {@code target} using a write-to-temp-then-atomic-rename
     * so a failure mid-write can never leave a half-written file that would be
     * discarded as corrupt on the next launch. The existing file (if any) is left
     * untouched until the new one is complete. Content is written as UTF-8.
     *
     * <p>Both persisted files (settings and the leaderboard) go through this one
     * helper so their durability guarantee cannot drift apart. Failures are
     * surfaced as {@link UncheckedIOException} so callers can warn the user.
     */
    public static void writeAtomically(Path target, List<String> lines) {
        Path tmp = null;
        try {
            ensureDataDir();
            tmp = Files.createTempFile(target.getParent(),
                    target.getFileName().toString(), ".tmp");
            Files.write(tmp, lines, StandardCharsets.UTF_8);
            // Replace the live file only once the temp file is fully written.
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                // Some filesystems reject ATOMIC_MOVE across the same directory;
                // fall back to a plain replace, which is still all-or-nothing at
                // the byte level compared with writing the target in place.
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            tmp = null;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + target, e);
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                    // Best-effort cleanup of the orphaned temp file.
                }
            }
        }
    }
}
