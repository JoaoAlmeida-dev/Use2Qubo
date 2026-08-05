package org.tzi.use.plugin.use2qubo.cli;

import org.tzi.use.plugin.use2qubo.qubo.engine.ProgressEvent;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Renders {@link ProgressEvent}s from {@code QuboEngine.derive} as a progress bar (interactive
 * terminal, {@code \r}-overwritten) or as throttled plain percentage lines (redirected/CI
 * output). ASCII-only except for the interactive multi-line block below, which uses minimal ANSI
 * cursor-movement codes (cursor-up + clear-line) to redraw one line per active
 * {@code SandboxWorkerPool} worker plus an aggregate "total" line — gated behind {@code
 * interactive}, which already requires a real console ({@link #isInteractiveStderr}), so plain/CI
 * output never sees an escape code. Never writes to {@code System.out}; never throws out of
 * {@link #accept}, so a rendering bug can never abort a multi-minute derivation.
 */
public final class ProgressBar implements Consumer<ProgressEvent> {

    private static final long PLAIN_MODE_MIN_INTERVAL_NANOS = 2_000_000_000L;

    private final PrintStream out;
    private final boolean interactive;

    private String lastLabel = null;
    private boolean barLineOpen = false;

    // --- interactive multi-line block state: one line per worker index (-1 = aggregate "total") ---
    private final Map<Integer, long[]> lineState = new LinkedHashMap<>(); // key -> [current, total]
    private final Map<Integer, Integer> lastLivePercentByKey = new HashMap<>();
    private int lastBlockHeight = 0;

    // --- plain-mode per-key throttling ---
    private final Map<Integer, Integer> lastPercentByKey = new HashMap<>();
    private final Map<Integer, Long> lastPrintNanosByKey = new HashMap<>();

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
            resetBatchState();
            return;
        }

        // Each sampled degree level (and each worker within it) shares one phaseLabel (e.g.
        // "Sampling cost degree 3"); reset per-batch state whenever it changes so a new batch's
        // first events always render, instead of being skipped/misplaced against the old batch's
        // worker set (which may have had a different worker count or already-open block).
        if (!e.phaseLabel.equals(lastLabel)) {
            closeBarLine();
            lastLabel = e.phaseLabel;
            resetBatchState();
        }

        if (interactive) {
            renderLiveBlock(e);
        } else {
            renderPeriodicLine(e);
        }
    }

    private void resetBatchState() {
        lineState.clear();
        lastBlockHeight = 0;
        lastPercentByKey.clear();
        lastPrintNanosByKey.clear();
        lastLivePercentByKey.clear();
    }

    private void renderLiveBlock(ProgressEvent e) {
        int evPercent = (int) Math.min(100, (100L * e.current) / e.total);
        Integer prevPercent = lastLivePercentByKey.get(e.workerIndex);
        lineState.put(e.workerIndex, new long[]{e.current, e.total});
        if (prevPercent != null && prevPercent == evPercent) return; // no visible change for this line
        lastLivePercentByKey.put(e.workerIndex, evPercent);

        if (lastBlockHeight > 0) {
            out.print("[" + lastBlockHeight + "A");
        }
        List<Integer> keys = new ArrayList<>(lineState.keySet());
        keys.sort((a, b) -> {
            if (a.equals(b)) return 0;
            if (a == -1) return 1;  // aggregate "total" line always last
            if (b == -1) return -1;
            return Integer.compare(a, b);
        });
        for (int key : keys) {
            long[] ct = lineState.get(key);
            int percent = (int) Math.min(100, (100L * ct[0]) / ct[1]);
            String label = key == -1 ? "total" : ("w" + key);
            out.print("\r[2K[use2qubo-cli][" + label + "] " + bar(percent) + " " + percent + "% ("
                    + QuboCli.abbreviate(ct[0]) + "/" + QuboCli.abbreviate(ct[1]) + ")\n");
        }
        out.flush();
        lastBlockHeight = keys.size();
        barLineOpen = true;
    }

    private static String bar(int percent) {
        int filled = percent / 10;
        StringBuilder bar = new StringBuilder(10);
        for (int i = 0; i < filled; i++) bar.append('=');
        if (filled < 10) bar.append('>');
        for (int i = bar.length(); i < 10; i++) bar.append('-');
        return "[" + bar + "]";
    }

    private void renderPeriodicLine(ProgressEvent e) {
        int percent = (int) Math.min(100, (100L * e.current) / e.total);
        Integer lastPercent = lastPercentByKey.get(e.workerIndex);
        long now = System.nanoTime();
        Long lastPrintNanos = lastPrintNanosByKey.get(e.workerIndex);
        boolean percentBucketChanged = lastPercent == null || percent / 10 != lastPercent / 10;
        boolean timeElapsed = lastPrintNanos == null || (now - lastPrintNanos) > PLAIN_MODE_MIN_INTERVAL_NANOS;
        if (!percentBucketChanged && !timeElapsed) return;

        lastPercentByKey.put(e.workerIndex, percent);
        lastPrintNanosByKey.put(e.workerIndex, now);
        String label = e.workerIndex == -1 ? e.phaseLabel : (e.phaseLabel + " [w" + e.workerIndex + "]");
        out.println("[use2qubo-cli] " + label + ": " + percent
                + "% (" + QuboCli.abbreviate(e.current) + "/" + QuboCli.abbreviate(e.total) + ")");
    }

    /** No-op beyond resetting the flag: unlike the old single-{@code \r}-line bar (which stayed
     *  mid-line and needed an extra newline before anything else could print), each interactive
     *  block line already ends in {@code \n} — the cursor is already correctly positioned. */
    private void closeBarLine() {
        barLineOpen = false;
    }
}
