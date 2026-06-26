package tessera.view;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * Generates short tone cues at runtime so the jar ships no audio files. Each cue
 * is a synthesised sine burst with a quick fade so it does not click. Sound is
 * opt-out via settings, and any audio-system failure is swallowed: a missing or
 * busy mixer must never interrupt play.
 */
public final class SoundPlayer {

    private static final float SAMPLE_RATE = 44_100f;
    private boolean enabled;

    public SoundPlayer(boolean enabled) {
        this.enabled = enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** A short rising blip when a tile is revealed. */
    public void flip() {
        play(660, 70);
    }

    /** A brighter two-note cue on a match. */
    public void match() {
        play(740, 80);
        play(990, 110);
    }

    /** A low, short cue on a mismatch. */
    public void mismatch() {
        play(300, 120);
    }

    /** A small ascending fanfare on a win. */
    public void win() {
        play(523, 110);
        play(659, 110);
        play(784, 160);
    }

    private void play(double frequencyHz, int durationMs) {
        if (!enabled) {
            return;
        }
        // Run off the EDT so synthesis never stalls the UI.
        Thread t = new Thread(() -> emit(frequencyHz, durationMs), "tessera-sound");
        t.setDaemon(true);
        t.start();
    }

    private void emit(double frequencyHz, int durationMs) {
        try {
            int frames = (int) (SAMPLE_RATE * durationMs / 1000.0);
            byte[] buffer = new byte[frames];
            int fade = Math.max(1, frames / 8);
            for (int i = 0; i < frames; i++) {
                double angle = 2.0 * Math.PI * i * frequencyHz / SAMPLE_RATE;
                double amplitude = 1.0;
                if (i < fade) {
                    amplitude = (double) i / fade;
                } else if (i > frames - fade) {
                    amplitude = (double) (frames - i) / fade;
                }
                buffer[i] = (byte) (Math.sin(angle) * 90 * amplitude);
            }
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                line.open(format);
                line.start();
                line.write(buffer, 0, buffer.length);
                line.drain();
            }
        } catch (Exception ignored) {
            // No mixer, or it is busy: cues are non-essential, so do nothing.
        }
    }
}
