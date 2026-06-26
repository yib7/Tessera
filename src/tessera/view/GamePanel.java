package tessera.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import tessera.controller.GameController;
import tessera.controller.GameView;
import tessera.controller.Navigator;
import tessera.model.Board;
import tessera.model.GameSession;
import tessera.model.Tile;

/**
 * The board screen. It builds the tile grid from the session, wires each tile's
 * click to the controller, and shows a live HUD (turns, score, time) plus pause
 * and quit controls. It implements {@link GameView} so the controller can drive
 * the animations without knowing this is Swing.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class GamePanel extends JPanel implements GameView {

    private final Navigator navigator;
    private final GameSession session;
    private final GameController controller;
    private final SoundPlayer sound;

    private final TileButton[][] tiles;
    private final JLabel turnsLabel = new JLabel();
    private final JLabel scoreLabel = new JLabel();
    private final JLabel timeLabel = new JLabel();
    private final JButton pauseButton = UiFactory.secondaryButton("Pause");
    private final JPanel boardPanel;
    private final Timer clockTimer;

    public GamePanel(Navigator navigator, GameSession session, SoundPlayer sound) {
        this.navigator = navigator;
        this.session = session;
        this.sound = sound;
        this.controller = new GameController(session, this);
        this.tiles = new TileButton[session.board().rows()][session.board().cols()];

        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 24, 24, 24));

        add(buildHud(), BorderLayout.NORTH);
        this.boardPanel = buildBoard();
        add(boardPanel, BorderLayout.CENTER);

        // Tick the time readout twice a second while the clock runs.
        this.clockTimer = new Timer(500, e -> refreshTime());
        clockTimer.start();

        updateHud();
    }

    public GameController controller() {
        return controller;
    }

    private JPanel buildHud() {
        JPanel hud = new JPanel(new BorderLayout());
        hud.setOpaque(false);
        hud.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JLabel title = new JLabel(session.size().label() + " board", SwingConstants.LEFT);
        title.setFont(Theme.heading());
        title.setForeground(Theme.TEXT_PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 24));

        JPanel stats = new JPanel();
        stats.setOpaque(false);
        stats.setLayout(new BoxLayout(stats, BoxLayout.X_AXIS));
        styleStat(turnsLabel);
        styleStat(scoreLabel);
        styleStat(timeLabel);
        stats.add(Box.createHorizontalGlue());
        stats.add(turnsLabel);
        stats.add(Box.createRigidArea(new Dimension(20, 0)));
        stats.add(scoreLabel);
        stats.add(Box.createRigidArea(new Dimension(20, 0)));
        stats.add(timeLabel);
        stats.add(Box.createHorizontalGlue());

        JPanel controls = new JPanel();
        controls.setOpaque(false);
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        pauseButton.addActionListener(e -> togglePause());
        JButton menuButton = UiFactory.secondaryButton("Quit to menu");
        menuButton.addActionListener(e -> {
            clockTimer.stop();
            navigator.show(Navigator.Screen.MENU);
        });
        controls.add(pauseButton);
        controls.add(Box.createRigidArea(new Dimension(10, 0)));
        controls.add(menuButton);

        hud.add(title, BorderLayout.WEST);
        hud.add(stats, BorderLayout.CENTER);
        hud.add(controls, BorderLayout.EAST);
        return hud;
    }

    private void styleStat(JLabel label) {
        label.setFont(Theme.hud());
        label.setForeground(Theme.TEXT_PRIMARY);
    }

    private JPanel buildBoard() {
        Board board = session.board();
        Color accent = Theme.fromRgb(session.theme().accentRgb());

        JPanel panel = new JPanel(new GridLayout(board.rows(), board.cols(), 8, 8));
        panel.setOpaque(false);
        for (int r = 0; r < board.rows(); r++) {
            for (int c = 0; c < board.cols(); c++) {
                final int row = r;
                final int col = c;
                Tile tile = board.tileAt(r, c);
                TileButton button = new TileButton(r, c, accent);
                button.setGlyph(tile.face());
                button.setStateImmediate(false, false);
                button.setOnClick(() -> controller.onTileClicked(row, col));
                tiles[r][c] = button;
                panel.add(button);
            }
        }
        return panel;
    }

    private void togglePause() {
        if (controller.isPaused()) {
            controller.resume();
            pauseButton.setText("Pause");
            clockTimer.start();
        } else {
            controller.pause();
            pauseButton.setText("Resume");
            clockTimer.stop();
        }
    }

    private void refreshTime() {
        long seconds = session.elapsedMillis() / 1000;
        timeLabel.setText(String.format("Time %d:%02d", seconds / 60, seconds % 60));
    }

    // GameView ---------------------------------------------------------------

    @Override
    public void flipUp(int row, int col, Runnable whenDone) {
        sound.flip();
        tiles[row][col].flipUp(whenDone);
    }

    @Override
    public void flipDown(int row, int col, Runnable whenDone) {
        tiles[row][col].flipDown(whenDone);
    }

    @Override
    public void markMatched(int row, int col) {
        sound.match();
        tiles[row][col].setMatched(true);
    }

    @Override
    public void setBoardInteractive(boolean interactive) {
        for (TileButton[] rowTiles : tiles) {
            for (TileButton tile : rowTiles) {
                tile.setInteractive(interactive);
            }
        }
    }

    @Override
    public void updateHud() {
        turnsLabel.setText("Turns " + session.turns());
        scoreLabel.setText("Score " + session.score());
        refreshTime();
        // A mismatch is the only HUD update that is not also a match; play its
        // cue here so the controller stays free of sound concerns.
        if (session.mismatches() > lastMismatchCount) {
            lastMismatchCount = session.mismatches();
            sound.mismatch();
        }
    }

    private int lastMismatchCount = 0;

    @Override
    public void onWin() {
        clockTimer.stop();
        sound.win();
        navigator.showResults(session);
    }
}
