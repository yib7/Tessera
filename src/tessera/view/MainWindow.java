package tessera.view;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

import tessera.controller.Navigator;
import tessera.model.Board;
import tessera.model.BoardSize;
import tessera.model.GameSession;
import tessera.model.Leaderboard;
import tessera.model.Settings;
import tessera.model.TileTheme;

/**
 * The single application window. It hosts every screen in a CardLayout and is
 * the one place that knows the navigation graph, which replaces the old
 * dispose-and-spawn-a-new-JFrame flow. Implementing {@link Navigator} lets every
 * panel ask to switch screens without reaching across to siblings.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class MainWindow extends JFrame implements Navigator {

    private final Settings settings;
    private final Leaderboard leaderboard;
    private final SoundPlayer sound;

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private final MenuPanel menuPanel;
    private final SettingsPanel settingsPanel;
    private final LeaderboardPanel leaderboardPanel;

    public MainWindow(Settings settings, Leaderboard leaderboard) {
        super("Tessera");
        this.settings = settings;
        this.leaderboard = leaderboard;
        this.sound = new SoundPlayer(settings.soundEnabled());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Minimum height is tuned so the largest board (HARD, 7 rows) keeps
        // legible tiles at the smallest allowed window: GridLayout divides the
        // available height evenly, so at 600px tall HARD tiles squashed to ~48px;
        // 660px keeps them ~57px (nearer the ~72px design and well above a usable
        // hit-target floor). Width is unchanged.
        setMinimumSize(new Dimension(760, 660));
        setSize(960, 720);

        this.menuPanel = new MenuPanel(this, settings);
        this.settingsPanel = new SettingsPanel(this, settings, sound, menuPanel::refresh);
        this.leaderboardPanel = new LeaderboardPanel(this, leaderboard);

        root.add(menuPanel, Screen.MENU.name());
        root.add(settingsPanel, Screen.SETTINGS.name());
        root.add(leaderboardPanel, Screen.LEADERBOARD.name());
        setContentPane(root);

        show(Screen.MENU);
        center();
    }

    @Override
    public void show(Screen screen) {
        switch (screen) {
            case MENU -> menuPanel.refresh();
            case SETTINGS -> settingsPanel.refresh();
            case LEADERBOARD -> leaderboardPanel.rebuild();
            default -> {
            }
        }
        cards.show(root, screen.name());
    }

    @Override
    public void startGame() {
        // A fresh random seed per game; captured on the session so the finished
        // run can be replayed on the identical layout.
        beginGame(settings.boardSize(), settings.tileTheme(), new Random().nextLong());
    }

    @Override
    public void replayGame(GameSession previous) {
        // Same size, theme, and seed as the finished run → the identical deal.
        beginGame(previous.size(), previous.theme(), previous.seed());
    }

    private void beginGame(BoardSize size, TileTheme theme, long seed) {
        Board board = new Board(size, theme, new Random(seed));
        GameSession session = new GameSession(size, theme, board, seed);
        // Detach any previous game panel first. Adding a new card under the same
        // name only replaces the CardLayout entry; the old panel stays attached
        // and keeps its clock Timer firing forever. Removing it fires its
        // removeNotify(), the single place that stops that panel's timers.
        disposePrevious(GamePanel.class);
        // Also drop any prior ResultsPanel now (symmetry with showResults). After
        // a finished game "Play again"/"Replay" comes through here, so without this
        // the just-shown ResultsPanel — and the session/board it pins — would
        // linger until the next showResults() call.
        disposePrevious(ResultsPanel.class);

        GamePanel gamePanel = new GamePanel(this, session, sound);
        root.add(gamePanel, Screen.GAME.name());
        cards.show(root, Screen.GAME.name());
    }

    /**
     * Remove every {@code root} child of the given type. For {@link GamePanel}
     * this fires each removed panel's removeNotify(), the single place that stops
     * that panel's timers; for {@link ResultsPanel} it prevents stale panels
     * pinning old sessions.
     */
    private void disposePrevious(Class<? extends Component> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                root.remove(child);
            }
        }
    }

    @Override
    public void showResults(GameSession session) {
        ResultsPanel results = new ResultsPanel(this, session, leaderboard);
        // Remove any prior ResultsPanel first; otherwise stale panels accumulate
        // under this card name and pin old sessions for the life of the process.
        disposePrevious(ResultsPanel.class);
        root.add(results, Screen.RESULTS.name());
        leaderboardPanel.showSize(session.size());
        cards.show(root, Screen.RESULTS.name());
    }

    @Override
    public void quit() {
        dispose();
        System.exit(0);
    }

    private void center() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - getWidth()) / 2, (screen.height - getHeight()) / 2);
    }
}
