# Credits

## Third-party code and assets

None. Tessera has no runtime dependencies beyond the Java standard library.

The look and feel is built from `java.awt` and `javax.swing` primitives:

- Tile faces are glyphs from the Unicode basic multilingual plane (letters,
  digits, and geometric symbols), drawn at runtime with `Graphics2D`. No image
  files are bundled.
- Sound cues are sine tones synthesised at runtime with
  `javax.sound.sampled`. No audio files are bundled.
- Fonts are resolved from the host system at startup, preferring a common
  sans-serif and falling back to the JVM's logical `SansSerif` family. No font
  files are bundled.

Because nothing is vendored, there are no third-party licenses to reproduce.
