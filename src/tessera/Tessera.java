package tessera;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import tessera.model.CrashLog;
import tessera.model.Leaderboard;
import tessera.model.Settings;
import tessera.view.MainWindow;
import tessera.view.Theme;

/**
 * Entry point. Loads persisted settings and high scores, applies the theme's
 * UIManager defaults, and shows the main window on the event dispatch thread.
 */
public final class Tessera {

    public static void main(String[] args) {
        // The game launches via javaw (no console), so an exception that escapes
        // before or after the window appears would otherwise vanish silently.
        // Record it to ~/.tessera/crash.log so "the game won't open" is triage-able.
        // The EDT routes its uncaught exceptions here too, so a failure inside
        // launch() is covered without a separate try/catch.
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                CrashLog.record(throwable, "Uncaught on thread \"" + thread.getName() + "\""));
        SwingUtilities.invokeLater(Tessera::launch);
    }

    private static void launch() {
        // Use the platform look and feel as a base, then layer Theme's defaults
        // on top. Falling through to the cross-platform L&F is harmless.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Keep the default look and feel.
        }
        Theme.install();

        Settings settings = Settings.load();
        Leaderboard leaderboard = new Leaderboard();
        new MainWindow(settings, leaderboard).setVisible(true);
    }
}
