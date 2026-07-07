package tessera.view;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;

import tessera.controller.Navigator;
import tessera.model.Board;
import tessera.model.GameSession;
import tessera.model.Leaderboard;
import tessera.model.Settings;

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
        setMinimumSize(new Dimension(760, 600));
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
        Board board = new Board(settings.boardSize(), settings.tileTheme());
        GameSession session = new GameSession(settings.boardSize(),
                settings.tileTheme(), board);
        // Detach any previous game panel first. Adding a new card under the same
        // name only replaces the CardLayout entry; the old panel stays attached
        // and keeps its clock Timer firing forever. Removing it fires its
        // removeNotify(), the single place that stops that panel's timers.
        disposePreviousGamePanel();

        GamePanel gamePanel = new GamePanel(this, session, sound);
        root.add(gamePanel, Screen.GAME.name());
        cards.show(root, Screen.GAME.name());
    }

    /** Remove any existing GamePanel so its removeNotify() stops its timers. */
    private void disposePreviousGamePanel() {
        for (Component child : root.getComponents()) {
            if (child instanceof GamePanel) {
                root.remove(child);
            }
        }
    }

    @Override
    public void showResults(GameSession session) {
        ResultsPanel results = new ResultsPanel(this, session, leaderboard);
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
