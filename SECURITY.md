# Security Policy

Tessera is an offline desktop game. It opens no network connections and handles
no credentials. The only data it writes is a leaderboard and a settings file
under a `.tessera` folder in your home directory.

The realistic risk surface is reading those local files at startup. The
leaderboard reader already tolerates a missing or malformed file and skips bad
lines rather than crashing. If you find input that still causes a crash or other
unexpected behavior, please report it.

## Reporting

Open a private security advisory on the repository (Security tab, "Report a
vulnerability"), or open an issue if the problem is not sensitive. Include the
file contents or steps that trigger it.
