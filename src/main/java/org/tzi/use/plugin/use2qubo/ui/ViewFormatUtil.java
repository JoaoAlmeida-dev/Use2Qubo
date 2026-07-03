package org.tzi.use.plugin.use2qubo.ui;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;

/**
 * Small formatting/color/label helpers shared across the QUBO result view panels
 * (matrix, sampling, graph tabs) — kept here instead of duplicated per panel.
 */
public final class ViewFormatUtil {

    private static final double COLOR_EPS = 1e-9;
    private static final Color POSITIVE = Color.GREEN;
    private static final Color NEGATIVE = Color.RED;

    private static final Color TERM_CONSTANT  = new Color(200, 200, 200);
    private static final Color TERM_LINEAR    = new Color(100, 149, 237);
    private static final Color TERM_QUADRATIC = new Color(218, 165, 32);

    private ViewFormatUtil() {}

    public static JLabel makeSwatch(Color c, String label) {
        JLabel l = new JLabel("■ " + label);
        l.setForeground(c);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    public static JLabel statLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    public static String abbrev(String s) {
        return s.length() > 20 ? s.substring(0, 20) + "…" : s;
    }

    public static String buildVectorTooltip(Object[] rowData, int n, List<String> varLabels) {
        StringBuilder sb = new StringBuilder("<html>");
        for (int i = 0; i < n; i++) {
            Object val = rowData[2 + i];
            if (val instanceof Integer && (Integer) val == 1) {
                String label = i < varLabels.size() ? varLabels.get(i) : "x" + i;
                sb.append(label).append("=1<br>");
            }
        }
        sb.append("</html>");
        return sb.toString();
    }

    public static String buildVectorAssignment(int[] vector, List<String> varLabels) {
        StringBuilder sb = new StringBuilder("<html>");
        for (int i = 0; i < vector.length; i++) {
            String label = i < varLabels.size() ? varLabels.get(i) : "x" + i;
            sb.append(label).append("=").append(vector[i]);
            if (i < vector.length - 1) sb.append("<br>");
        }
        sb.append("</html>");
        return sb.toString();
    }

    /** Magnitude-based colour interpolation for matrix cells: white at zero, blending toward
     *  blue (positive) or red (negative) as |value| approaches maxAbs. */
    public static Color colorForValue(double v, double maxAbs) {
        if (v == 0.0 || maxAbs < COLOR_EPS) return Color.WHITE;
        double alpha = Math.min(1.0, Math.max(0.05, Math.abs(v) / maxAbs));
        Color target = v > 0.0 ? POSITIVE : NEGATIVE;
        return blend(Color.WHITE, target, alpha);
    }

    /** Colour for a sampling-tab term type, derived from a {@code SampleRecord}'s derivedI/derivedJ. */
    public static Color termTypeColor(int derivedI, int derivedJ) {
        if (derivedI == -1) return TERM_CONSTANT;
        if (derivedI == derivedJ) return TERM_LINEAR;
        return TERM_QUADRATIC;
    }

    private static Color blend(Color from, Color to, double alpha) {
        int r = (int) (from.getRed()   + alpha * (to.getRed()   - from.getRed()));
        int g = (int) (from.getGreen() + alpha * (to.getGreen() - from.getGreen()));
        int b = (int) (from.getBlue()  + alpha * (to.getBlue()  - from.getBlue()));
        return new Color(r, g, b);
    }

    /** Wires a search box to filter {@code table}'s rows by substring match against any column. */
    public static void installRowFilter(JTextField searchBox, JTable table) {
        TableRowSorter<javax.swing.table.TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        searchBox.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() {
                String text = searchBox.getText().trim();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
                }
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });
    }
}
