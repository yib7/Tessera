# Changelog

All notable changes to Tessera are recorded here. This project follows
[semantic versioning](https://semver.org).

## v1.0.0 - 2026-06-27

First public release.

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
