package tessera.model;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A tiny best-effort crash log under the Tessera data directory
 * ({@code ~/.tessera/crash.log}). Because the game launches via {@code javaw}
 * (no console) there is otherwise no diagnostic trail when it fails before the
 * window appears; this gives "the game won't open" reports something to attach.
 *
 * <p>Installed as the default uncaught-exception handler in
 * {@link tessera.Tessera#main}. Writing is append-only and any failure is
 * swallowed — crash logging must never itself throw.
 */
public final class CrashLog {

    private static final String FILE_NAME = "crash.log";

    private CrashLog() {
    }

    /** Append a crash entry to {@code ~/.tessera/crash.log}; never throws. */
    public static void record(Throwable throwable, String context) {
        try {
            DataPaths.ensureDataDir();
        } catch (IOException ignored) {
            // If the data dir cannot be created there is nowhere to log; give up
            // quietly rather than let the crash handler itself throw.
            return;
        }
        record(DataPaths.dataDir().resolve(FILE_NAME), throwable, context);
    }

    /**
     * Append a crash entry to a specific file (test seam). Best-effort: creates
     * the parent directory if needed and swallows any I/O failure.
     */
    public static void record(Path file, Throwable throwable, String context) {
        String entry = format(throwable, context, ZonedDateTime.now());
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, entry, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Nowhere to record to; there is nothing left to recover to.
        }
    }

    private static String format(Throwable throwable, String context, ZonedDateTime when) {
        StringBuilder sb = new StringBuilder();
        sb.append("==== ")
                .append(when.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .append(" ====").append(System.lineSeparator());
        if (context != null && !context.isBlank()) {
            sb.append("Context: ").append(context).append(System.lineSeparator());
        }
        if (throwable != null) {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            sb.append(sw);
        }
        sb.append(System.lineSeparator());
        return sb.toString();
    }
}
