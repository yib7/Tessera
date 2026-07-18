package tessera.view;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * Generates short tone cues at runtime so the jar ships no audio files. Each cue
 * is a synthesised sine burst with a quick fade so it does not click. Sound is
 * opt-out via settings, and any audio-system failure is swallowed: a missing or
 * busy mixer must never interrupt play.
 *
 * <p>Cues play on short daemon threads, which the JVM would otherwise kill
 * mid-write at exit and leave the mixer line held open. A shutdown hook closes
 * any line still open at exit so the mixer is released cleanly.
 */
public final class SoundPlayer {

    private static final float SAMPLE_RATE = 44_100f;
    // Peak 8-bit sample amplitude at full volume (headroom below 127 avoids clipping).
    private static final double PEAK_AMPLITUDE = 90.0;
    private boolean enabled;
    private volatile int volume = 100; // 0-100; scales the synth amplitude

    // Lines currently open on cue threads, so the shutdown hook can close them.
    private final Set<SourceDataLine> openLines = ConcurrentHashMap.newKeySet();

    public SoundPlayer(boolean enabled) {
        this.enabled = enabled;
        Runtime.getRuntime().addShutdownHook(new Thread(this::closeOpenLines,
                "tessera-sound-shutdown"));
    }

    private void closeOpenLines() {
        for (SourceDataLine line : openLines) {
            try {
                line.close();
            } catch (RuntimeException ignored) {
                // Best-effort release on exit; nothing left to recover to.
            }
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Set cue loudness, 0-100 (clamped). Applies to subsequent cues. */
    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(100, volume));
    }

    /** A short rising blip when a tile is revealed. */
    public void flip() {
        play(new int[] {660}, new int[] {70});
    }

    /** A brighter two-note cue on a match. */
    public void match() {
        play(new int[] {740, 990}, new int[] {80, 110});
    }

    /** A low, short cue on a mismatch. */
    public void mismatch() {
        play(new int[] {300}, new int[] {120});
    }

    /** A small ascending fanfare on a win. */
    public void win() {
        play(new int[] {523, 659, 784}, new int[] {110, 110, 160});
    }

    /**
     * Play a cue as a sequence of tones on a single daemon thread, so a
     * multi-note cue sounds as notes one after another instead of stacked on top
     * of each other. Running off the EDT keeps synthesis from stalling the UI.
     */
    private void play(int[] frequenciesHz, int[] durationsMs) {
        if (!enabled) {
            return;
        }
        Thread t = new Thread(() -> render(frequenciesHz, durationsMs), "tessera-sound");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Render a whole cue on ONE mixer line: acquire a single
     * {@link SourceDataLine}, write every tone of the cue to it back to back,
     * then drain and close. Reusing one line for the cue avoids a fresh line
     * acquisition per note, which could otherwise leave small audible gaps inside
     * a multi-note fanfare and wastes open/close work.
     */
    private void render(int[] frequenciesHz, int[] durationsMs) {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                openLines.add(line);
                try {
                    line.open(format);
                    line.start();
                    for (int i = 0; i < frequenciesHz.length; i++) {
                        byte[] buffer = tone(frequenciesHz[i], durationsMs[i]);
                        line.write(buffer, 0, buffer.length);
                    }
                    line.drain();
                } finally {
                    openLines.remove(line);
                }
            }
        } catch (Exception ignored) {
            // No mixer, or it is busy: cues are non-essential, so do nothing.
        }
    }

    /** Synthesise one fading sine-burst tone into an 8-bit PCM buffer. */
    private byte[] tone(double frequencyHz, int durationMs) {
        int frames = (int) (SAMPLE_RATE * durationMs / 1000.0);
        byte[] buffer = new byte[frames];
        int fade = Math.max(1, frames / 8);
        double peak = PEAK_AMPLITUDE * (volume / 100.0);
        for (int i = 0; i < frames; i++) {
            double angle = 2.0 * Math.PI * i * frequencyHz / SAMPLE_RATE;
            double amplitude = 1.0;
            if (i < fade) {
                amplitude = (double) i / fade;
            } else if (i > frames - fade) {
                amplitude = (double) (frames - i) / fade;
            }
            buffer[i] = (byte) (Math.sin(angle) * peak * amplitude);
        }
        return buffer;
    }
}
