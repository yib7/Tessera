package tessera.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * Player preferences that persist between launches: board size, tile theme, and
 * whether sound cues play. Stored as a small properties file under the Tessera
 * data directory. Reads tolerate a missing or malformed file by falling back to
 * defaults, so a fresh install or a corrupt write never blocks startup.
 */
public final class Settings {

    private static final String FILE_NAME = "settings.properties";
    private static final String KEY_SIZE = "board.size";
    private static final String KEY_THEME = "tile.theme";
    private static final String KEY_SOUND = "sound.enabled";

    private BoardSize boardSize = BoardSize.NORMAL;
    private TileTheme tileTheme = TileTheme.LETTERS;
    private boolean soundEnabled = true;

    public BoardSize boardSize() {
        return boardSize;
    }

    public void setBoardSize(BoardSize boardSize) {
        this.boardSize = boardSize;
    }

    public TileTheme tileTheme() {
        return tileTheme;
    }

    public void setTileTheme(TileTheme tileTheme) {
        this.tileTheme = tileTheme;
    }

    public boolean soundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    /** Load preferences, falling back to defaults on any read problem. */
    public static Settings load() {
        return load(DataPaths.dataDir().resolve(FILE_NAME));
    }

    /** Load preferences from a specific file (test seam), defaults on any read problem. */
    public static Settings load(Path path) {
        Settings settings = new Settings();
        if (!Files.exists(path)) {
            return settings;
        }
        Properties props = new Properties();
        try (var reader = Files.newBufferedReader(path)) {
            props.load(reader);
            settings.boardSize = BoardSize.fromLabel(props.getProperty(KEY_SIZE));
            settings.tileTheme = TileTheme.fromName(props.getProperty(KEY_THEME));
            settings.soundEnabled = Boolean.parseBoolean(
                    props.getProperty(KEY_SOUND, "true"));
        } catch (IOException | RuntimeException e) {
            // Corrupt or unreadable file: keep the defaults already set.
            return new Settings();
        }
        return settings;
    }

    /**
     * Persist preferences via {@link DataPaths#writeAtomically}, which uses a
     * write-to-temp-then-atomic-rename so a failure mid-write can never leave a
     * half-written {@code settings.properties} that would be discarded as corrupt
     * on the next launch. The existing file (if any) is left untouched until the
     * new one is complete. Failures are surfaced as unchecked so callers can warn
     * the user; settings are not load-bearing for play, so a caller may also
     * choose to ignore them.
     *
     * <p>The content is built as a plain {@code key=value} list (with a leading
     * comment) rather than {@link Properties#store}, so the leaderboard and
     * settings share the one atomic writer. {@link #load()} parses this with
     * {@link Properties#load}, which reads {@code key=value} lines and ignores
     * {@code #} comments, so the format round-trips.
     */
    public void save() {
        save(DataPaths.dataDir().resolve(FILE_NAME));
    }

    /** Persist preferences to a specific file (test seam). */
    public void save(Path path) {
        List<String> lines = List.of(
                "# Tessera settings",
                KEY_SIZE + "=" + boardSize.label(),
                KEY_THEME + "=" + tileTheme.displayName(),
                KEY_SOUND + "=" + Boolean.toString(soundEnabled));
        DataPaths.writeAtomically(path, lines);
    }
}
