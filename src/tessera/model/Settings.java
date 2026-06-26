package tessera.model;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        Settings settings = new Settings();
        Path path = DataPaths.dataDir().resolve(FILE_NAME);
        if (!Files.exists(path)) {
            return settings;
        }
        Properties props = new Properties();
        try {
            props.load(Files.newBufferedReader(path));
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

    /** Persist preferences. Failures are surfaced as unchecked so callers can
     *  choose to ignore them; settings are not load-bearing for play. */
    public void save() {
        Properties props = new Properties();
        props.setProperty(KEY_SIZE, boardSize.label());
        props.setProperty(KEY_THEME, tileTheme.displayName());
        props.setProperty(KEY_SOUND, Boolean.toString(soundEnabled));
        Path path = DataPaths.dataDir().resolve(FILE_NAME);
        try {
            DataPaths.ensureDataDir();
            props.store(Files.newBufferedWriter(path), "Tessera settings");
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save settings to " + path, e);
        }
    }
}
