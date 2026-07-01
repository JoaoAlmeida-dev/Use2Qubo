package org.tzi.use.plugin.use2qubo.ui;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import javax.swing.JLabel;

/**
 * Small formatting/label helpers shared across the QUBO result view panels
 * (matrix, sampling, graph tabs) — kept here instead of duplicated per panel.
 */
final class ViewFormatUtil {

    private ViewFormatUtil() {}

    static JLabel makeSwatch(Color c, String label) {
        JLabel l = new JLabel("■ " + label);
        l.setForeground(c);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    static JLabel statLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    static String abbrev(String s) {
        return s.length() > 20 ? s.substring(0, 20) + "…" : s;
    }

    static String buildVectorTooltip(Object[] rowData, int n, List<String> varLabels) {
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

    static String buildVectorAssignment(int[] vector, List<String> varLabels) {
        StringBuilder sb = new StringBuilder("<html>");
        for (int i = 0; i < vector.length; i++) {
            String label = i < varLabels.size() ? varLabels.get(i) : "x" + i;
            sb.append(label).append("=").append(vector[i]);
            if (i < vector.length - 1) sb.append("<br>");
        }
        sb.append("</html>");
        return sb.toString();
    }
}
