# Changelog

All notable changes to Tessera are recorded here. This project follows
[semantic versioning](https://semver.org).

## v3.3.0 - 2026-07-14

A full visual restyle from the dark slate theme to a bright, tactile look.
Everything is still hand-painted in Graphics2D with no image files, no UI
libraries, and JDK-safe fonts. Gameplay, scoring, and persistence are unchanged.

### Added

- Mismatch feedback: two tiles that do not match flash a red border and shake,
  so a wrong guess reads at a glance instead of silently flipping back.
- A green glow when a pair matches, and a diagonal cascade when the board deals
  face down to start a round.
- A celebration on the results screen, and a designed empty state for a
  leaderboard size that has no scores yet.

### Changed

- Repainted every screen and control on the bright palette: tiles with distinct
  face-down, hover, keyboard-focus, face-up, matched, and mismatch states;
  gradient buttons; and custom pickers, a dropdown, toggle, text field, chips,
  and leaderboard table in place of the stock Swing controls.
- The tile flip is now a time-based ease-in-out with a brightness veil, replacing
  the old linear squash.

## v3.2.0 - 2026-07-12

### Added

- The board hides behind a cover panel while paused, so pausing mid-turn can no
  longer be used to study a revealed tile with the clock frozen.
- Full keyboard play: arrow keys move focus across the tile grid, Enter or
  Space flips the focused tile, and Escape toggles pause.
- The memorize countdown now scales with board size instead of a flat five
  seconds.
- A "Play again" button on the results screen when a run qualifies for the
  leaderboard.
- A warning dialog when a score fails to save to disk, matching the existing
  settings-save warning.

### Fixed

- CI's jar-packaging step now creates `dist/` before writing to it.
- Leaderboard and settings saves both go through one shared write-temp-then-
  atomic-rename helper (`DataPaths.writeAtomically`), removing a data-loss
  window on a crash or full disk mid-write.
- `Leaderboard.qualifies` now uses the same score/turns/time ordering as
  ranking, so a run that ties the last qualifying score with fewer turns or a
  faster time is offered a name entry instead of being silently dropped.
- Corrupt leaderboard lines with an unrecognized board size or a negative
  score, turn count, or time are now dropped instead of being re-bucketed
  onto the Normal board.
- Settings tile faces and combo boxes on the settings screen show display
  names ("Easy", "Numbers") instead of raw enum constants.
- Tile clicks now register on mouse press instead of mouse click, so a small
  hand tremor between press and release no longer swallows the input.
- Stale `ResultsPanel` instances are removed from the card container instead
  of accumulating for the life of the process.
- The mismatch-resolution timer is now owned by the controller and cancelled
  on teardown, instead of continuing to run against a discarded game.
- The game clock uses a monotonic time source instead of wall-clock time, so
  it can no longer be corrupted by a system clock adjustment mid-game.

## v3.1.0 - 2026-06-27

### Added

- A memorize phase before each round: the board opens face up for a few seconds
  with an on-screen countdown, then flips down to begin play. Input and pause are
  held until it ends, and the clock still starts on the first flip, so the
  memorize time never counts against the score.

## v3.0.0 - 2026-06-27

A ground-up revamp that rebuilds and renames the old Java GUI Memory Game into
Tessera: one Swing window, a clean model/view/controller core, scoring, tile
themes, a flip animation, and a persistent leaderboard.

### Added

- Single-window UI with menu, settings, game, results, and leaderboard screens on
  a `CardLayout`.
- Three board sizes (Easy 3x4, Normal 4x7, Hard 7x8) and three code-drawn tile
  themes (letters, numbers, symbols), so the jar bundles no image files.
- Scoring that rewards matched pairs, penalizes mismatches, and adds a speed bonus
  that decays over time.
- Custom-painted tiles with a horizontal-squash flip animation, a live HUD, and
  pause/resume that stops the clock.
- A persistent top-five leaderboard per board size, stored under `~/.tessera`, with
  a reader that tolerates a missing or corrupt file.
- Runtime-synthesized sound cues, with no audio files bundled.
- Build and run scripts for Windows, macOS, and Linux, plus a GitHub Actions CI
  workflow that compiles the sources and runs the logic tests.

## Earlier releases

Versions V1.0 through V2.0 shipped under the project's previous name. See the
[GitHub releases](https://github.com/yib7/Tessera/releases) for those.
