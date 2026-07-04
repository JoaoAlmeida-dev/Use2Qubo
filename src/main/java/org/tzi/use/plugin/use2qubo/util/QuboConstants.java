package org.tzi.use.plugin.use2qubo.util;

/**
 * Named constants for values that were previously inline magic numbers in the
 * QUBO derivation and graph-rendering code. Grouped by the class that uses them.
 */
public final class QuboConstants {

    private QuboConstants() {}

    // --- QuboEngine: coefficient rounding / exactness check ---

    /** Coefficients smaller than this magnitude are treated as zero. */
    public static final double EPS = 1e-9;

    /** Number of held-out points evaluated by the exactness check. */
    public static final int EXACTNESS_SAMPLE_COUNT = 20;

    /** Fixed seed so exactness-check held-out vectors are reproducible across runs. */
    public static final long EXACTNESS_SEED = 7919L;

    /** Held-out vectors need at least this many 1-bits (lower weights are training samples, exact by construction). */
    public static final int EXACTNESS_MIN_HAMMING_WEIGHT = 3;

    /** Floor on the retry budget when searching for qualifying held-out vectors. */
    public static final int EXACTNESS_MIN_ATTEMPTS = 200;

    /** Retry budget scales with n^2 so small models still get a fair search; multiplier per n^2. */
    public static final int EXACTNESS_ATTEMPTS_PER_N_SQUARED = 20;

    /** Default cap on pseudo-Boolean polynomial degree explored when degree-2 sampling is not exact
     *  (AutoQUBO §4.3 escalation); overridable per-model via {@code max_degree} in qubo_config.json. */
    public static final int DEFAULT_MAX_POLY_DEGREE = 3;

    /** Rosenberg quadratization penalty margin added on top of the per-pair coefficient bound,
     *  so the ancilla-consistency penalty strictly dominates (Rosenberg 1975; Dattani 2019 survey). */
    public static final double QUADRATIZATION_PENALTY_MARGIN = 1.0;

    // --- QuboGraphPanel: node/edge sizing ---

    /** Extra edge-width multiplier applied on top of the base width, scaled by |coefficient| / maxQuad. */
    public static final double GRAPH_EDGE_WIDTH_SCALE = 4.0;

    /** Minimum node radius (px), before the magnitude-based scale term is added. */
    public static final double GRAPH_NODE_RADIUS_BASE = 12.0;

    /** Additional node radius (px) at maximum |linear coefficient|. */
    public static final double GRAPH_NODE_RADIUS_SCALE = 8.0;

    /** Extra pixels added to a node's radius for tooltip/click hit-testing. */
    public static final double GRAPH_NODE_HIT_PAD = 5.0;

    /** Maximum pixel distance from an edge's line segment still counted as a hit. */
    public static final double GRAPH_EDGE_HIT_TOLERANCE = 15.0;
}
