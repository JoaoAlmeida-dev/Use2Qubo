package org.tzi.use.plugin.use2qubo.util;

import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PluginLog {

    private static final Logger JUL = Logger.getLogger("org.tzi.use.plugin.use2qubo");
    private static volatile PrintWriter useWriter;

    private PluginLog() {}

    /** Wire up the USE log panel. Call once at the start of each plugin action. */
    public static void init(PrintWriter writer) { useWriter = writer; }

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
