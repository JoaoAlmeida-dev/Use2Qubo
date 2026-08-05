package org.tzi.use.plugin.use2qubo.cli;

import org.tzi.use.plugin.use2qubo.qubo.engine.ProgressEvent;

import java.io.PrintStream;
import java.util.function.Consumer;

/**
 * Renders {@link ProgressEvent}s from {@code QuboEngine.derive} as a progress bar (interactive
 * terminal, {@code \r}-overwritten) or as throttled plain percentage lines (redirected/CI
 * output). ASCII-only rendering; never writes to {@code System.out}; never throws out of
 * {@link #accept}, so a rendering bug can never abort a multi-minute derivation.
 */
public final class ProgressBar implements Consumer<ProgressEvent> {

    private static final long PLAIN_MODE_MIN_INTERVAL_NANOS = 2_000_000_000L;

    private final PrintStream out;
    private final boolean interactive;

    private String lastLabel = null;
    private int lastPercent = -1;
    private long lastPrintNanos = 0;
    private boolean barLineOpen = false;

    public ProgressBar(PrintStream out, boolean interactive) {
        this.out = out;
        this.interactive = interactive;
    }

    /** No-op progress bar: renders nothing. Used when progress display is explicitly disabled. */
    public static ProgressBar disabled() {
        return new ProgressBar(null, false);
    }

    public static ProgressBar forStderr() {
        String forced = System.getProperty("use2qubo.progress");
        if ("off".equals(forced)) {
            return disabled();
        }
        return new ProgressBar(System.err, isInteractiveStderr());
    }

    /**
     * Whether stderr should be treated as an interactive terminal (live {@code \r} bar) versus
     * redirected/non-interactive output (throttled plain lines). Checks the
     * {@code -Duse2qubo.progress=bar|plain|off} override first (also the test injection point),
     * then falls back to {@code System.console() != null} — a heuristic known to be unreliable on
     * Windows/PowerShell (returns {@code null} whenever any stream is redirected, and inconsistent
     * under some terminal/IDE integrations), but the only zero-dependency signal available.
     */
    static boolean isInteractiveStderr() {
        String forced = System.getProperty("use2qubo.progress");
        if ("bar".equals(forced)) return true;
        if ("plain".equals(forced) || "off".equals(forced)) return false;
        return System.console() != null;
    }

    @Override
    public void accept(ProgressEvent e) {
        if (out == null) return;
        try {
            render(e);
        } catch (RuntimeException ignored) {
            // A rendering bug must never abort a multi-minute derivation.
        }
    }

    private void render(ProgressEvent e) {
        if (e.total <= 0) {
            closeBarLine();
            out.println("[use2qubo-cli] " + e.phaseLabel);
            lastLabel = null;
            lastPercent = -1;
            return;
        }

        // Each sampled degree level has its own phaseLabel (e.g. "Sampling cost degree 3"); reset
        // the percent tracker whenever it changes so a new degree's first samples always render,
        // instead of being skipped because the previous degree happened to end at the same %.
        if (!e.phaseLabel.equals(lastLabel)) {
            lastLabel = e.phaseLabel;
            lastPercent = -1;
        }

        int percent = (int) Math.min(100, (100L * e.current) / e.total);
        if (interactive) {
            renderLiveBar(e.phaseLabel, percent, e.current, e.total);
        } else {
            renderPeriodicLine(e.phaseLabel, percent, e.current, e.total);
        }
    }

    private void renderLiveBar(String label, int percent, long current, long total) {
        if (percent == lastPercent) return;
        lastPercent = percent;
        int filled = percent / 10;
        StringBuilder bar = new StringBuilder(10);
        for (int i = 0; i < filled; i++) bar.append('=');
        if (filled < 10) bar.append('>');
        for (int i = bar.length(); i < 10; i++) bar.append('-');

        out.print("\r[use2qubo-cli] " + label
                + " [" + bar + "] " + percent + "% ("
                + QuboCli.abbreviate(current) + "/" + QuboCli.abbreviate(total) + ")");
        out.flush();
        barLineOpen = true;
    }

    private void renderPeriodicLine(String label, int percent, long current, long total) {
        long now = System.nanoTime();
        boolean percentBucketChanged = lastPercent < 0 || percent / 10 != lastPercent / 10;
        boolean timeElapsed = (now - lastPrintNanos) > PLAIN_MODE_MIN_INTERVAL_NANOS;
        if (!percentBucketChanged && !timeElapsed) return;

        lastPercent = percent;
        lastPrintNanos = now;
        out.println("[use2qubo-cli] " + label + ": " + percent
                + "% (" + QuboCli.abbreviate(current) + "/" + QuboCli.abbreviate(total) + ")");
    }

    private void closeBarLine() {
        if (barLineOpen) {
            out.println();
            barLineOpen = false;
        }
    }
}
