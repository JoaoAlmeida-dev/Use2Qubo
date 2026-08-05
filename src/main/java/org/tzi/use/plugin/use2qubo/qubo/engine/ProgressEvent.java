package org.tzi.use.plugin.use2qubo.qubo.engine;

/**
 * Structured progress signal for CLI/GUI progress rendering, threaded alongside (not replacing)
 * the existing free-form {@code Consumer<String>} progress callback.
 *
 * <p>{@code total <= 0} means "one-off phase message, no count/total to render" (e.g. "Building
 * variable index", "Compiling objective OCL", "Running exactness check (degree N)"). {@code total
 * > 0} means a determinate sub-progress within the current phase (e.g. sample count within a
 * degree level: {@code current}/{@code total} = count/C(n,m)).
 */
public final class ProgressEvent {
    public final String phaseLabel;
    public final String samplePrefix; // "cost" | "pen" | null for non-sampling phases
    public final int degree;          // -1 if not degree-scoped
    public final long current;
    public final long total;          // <=0 => indeterminate/one-off phase message

    public ProgressEvent(String phaseLabel, String samplePrefix, int degree, long current, long total) {
        this.phaseLabel = phaseLabel;
        this.samplePrefix = samplePrefix;
        this.degree = degree;
        this.current = current;
        this.total = total;
    }

    public static ProgressEvent phase(String label) {
        return new ProgressEvent(label, null, -1, 0, 0);
    }
}
