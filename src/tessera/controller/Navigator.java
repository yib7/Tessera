package tessera.controller;

import tessera.model.GameSession;

/**
 * The screens the main window can show and how panels ask to move between them.
 * Panels never reach for other panels or frames directly; they call back through
 * this interface, which keeps the navigation graph in one place (the main
 * window) instead of scattered across "dispose this, new that" calls.
 */
public interface Navigator {

    enum Screen { MENU, SETTINGS, GAME, RESULTS, LEADERBOARD }

    /** Show a screen that needs no extra context. */
    void show(Screen screen);

    /** Start a new game with the current settings and show the board. */
    void startGame();

    /** Show the results screen for a finished session. */
    void showResults(GameSession session);

    /** Quit the application. */
    void quit();
}
