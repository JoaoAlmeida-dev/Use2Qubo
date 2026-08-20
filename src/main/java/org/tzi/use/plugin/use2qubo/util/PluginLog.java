package org.tzi.use.plugin.use2qubo.util;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Static logging facade shared across the plugin: mirrors every INFO+ message to USE's own
 * log panel (via the {@link PrintWriter} wired up by {@link #init}) and always to the
 * {@code java.util.logging} logger {@code org.tzi.use.plugin.use2qubo}, so the same call sites
 * work identically in the Swing plugin and the headless {@link org.tzi.use.plugin.use2qubo.cli.QuboCli}.
 * {@link #init} must be called once per plugin action before logging (the CLI wires stderr instead).
 */
public final class PluginLog {

    private static final Logger JUL = Logger.getLogger("org.tzi.use.plugin.use2qubo");
    private static final AtomicBoolean FILE_LOGGING_INSTALLED = new AtomicBoolean(false);
    private static volatile PrintWriter useWriter;

    private PluginLog() {}

    /** Wire up the USE log panel. Call once at the start of each plugin action. */
    public static void init(PrintWriter writer) {
        useWriter = writer;
        installFileHandler();
    }

    /** Attaches a per-day log file handler to {@link #JUL}, once per JVM lifetime.
     *  Falls back to stderr-only logging (never throws) if {@code ./logs/} can't be created. */
    private static void installFileHandler() {
        if (!FILE_LOGGING_INSTALLED.compareAndSet(false, true)) return;
        try {
            Files.createDirectories(Paths.get("logs"));
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            FileHandler handler = new FileHandler("logs/" + today + "-use2qubo.log", true);
            handler.setLevel(Level.ALL);
            handler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return "[use2qubo] " + record.getLevel() + ": " + formatMessage(record) + System.lineSeparator();
                }
            });
            JUL.addHandler(handler);
        } catch (Exception e) {
            System.err.println("[use2qubo] WARNING: could not set up file logging under ./logs/: " + e.getMessage());
        }
    }

    public static void info(String msg)               { log(Level.INFO,    msg, null); }
    public static void warn(String msg)               { log(Level.WARNING, msg, null); }
    public static void warn(String msg, Throwable t)  { log(Level.WARNING, msg, t);    }
    public static void error(String msg, Throwable t) { log(Level.SEVERE,  msg, t);    }
    public static void debug(String msg)              { log(Level.FINE,    msg, null); }

    private static void log(Level level, String msg, Throwable t) {
        PrintWriter w = useWriter;
        if (w != null && level.intValue() >= Level.INFO.intValue()) {
            w.println("[use2qubo] " + level.getName() + ": " + msg);
            if (t != null) t.printStackTrace(w);
            w.flush();
        }
        if (t != null) JUL.log(level, msg, t);
        else           JUL.log(level, msg);
    }
}
