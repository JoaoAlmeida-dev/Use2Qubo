package org.tzi.use.plugin.use2qubo.ui;

import java.awt.Color;

/**
 * Magnitude-based colour interpolation shared by matrix-cell rendering:
 * white at zero, blending toward blue (positive) or red (negative) as
 * |value| approaches maxAbs.
 */
final class CellColorScale {

    private static final double EPS = 1e-9;
    private static final Color POSITIVE = new Color(100, 149, 237); // cornflower blue
    private static final Color NEGATIVE = new Color(205, 92, 92);   // indian red

    private CellColorScale() {}

    static Color forValue(double v, double maxAbs) {
        if (v == 0.0 || maxAbs < EPS) return Color.WHITE;
        double alpha = Math.min(1.0, Math.max(0.05, Math.abs(v) / maxAbs));
        Color target = v > 0.0 ? POSITIVE : NEGATIVE;
        return blend(Color.WHITE, target, alpha);
    }

    private static Color blend(Color from, Color to, double alpha) {
        int r = (int) (from.getRed()   + alpha * (to.getRed()   - from.getRed()));
        int g = (int) (from.getGreen() + alpha * (to.getGreen() - from.getGreen()));
        int b = (int) (from.getBlue()  + alpha * (to.getBlue()  - from.getBlue()));
        return new Color(r, g, b);
    }
}
