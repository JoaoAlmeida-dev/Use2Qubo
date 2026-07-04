package org.tzi.use.plugin.use2qubo.ui.tabs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.tzi.use.plugin.use2qubo.qubo.QuboResult;
import org.tzi.use.plugin.use2qubo.qubo.SampleRecord;
import org.tzi.use.plugin.use2qubo.ui.ViewFormatUtil;

/** "Sampling" tab: cost/penalty AutoQUBO probe tables, one per pass, with term-type colouring,
 *  probe-count summaries, search filtering, and click-through to the matching Q-matrix cell. */
public class SamplingTabPanel extends JSplitPane {

    public SamplingTabPanel(QuboResult result, MatrixTabPanel matrixTabPanel, Runnable switchToMatrixTab) {
        super(JSplitPane.HORIZONTAL_SPLIT,
                buildPassPanel("Cost pass", result.costSamples, result, matrixTabPanel, switchToMatrixTab),
                buildPassPanel("Penalty pass", result.penaltySamples, result, matrixTabPanel, switchToMatrixTab));
        setResizeWeight(0.5);
    }

    private static JPanel buildPassPanel(String title, List<SampleRecord> samples,
            QuboResult result, MatrixTabPanel matrixTabPanel, Runnable switchToMatrixTab) {
        // Sample vectors are always over the original decision variables (never ancillas, which
        // only exist post-quadratization) — do not use result.nVars here once ancillas are present.
        int n = samples.isEmpty() ? result.nVars : samples.get(0).vector.length;

        int constCount = 0, linearCount = 0, quadCount = 0;
        // Count of probes per degree ≥ 3, keyed by degree (3, 4, ...).
        java.util.Map<Integer, Integer> higherOrderCounts = new java.util.TreeMap<>();
        for (SampleRecord sr : samples) {
            if (sr.derivedI == -1) constCount++;
            else if (sr.derivedI == -2) {
                higherOrderCounts.merge(sr.termVars.length, 1, Integer::sum);
            } else if (sr.derivedI == sr.derivedJ) linearCount++;
            else quadCount++;
        }
        int expectedLinear = n;
        int expectedQuad = n * (n - 1) / 2;

        JTable table = buildTable(samples, result, matrixTabPanel, switchToMatrixTab);

        JTextField search = new JTextField();
        ViewFormatUtil.installRowFilter(search, table);

        JPanel summary = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        summary.add(ViewFormatUtil.statLabel("const: " + constCount + "/1"));
        summary.add(countLabel("linear", linearCount, expectedLinear));
        summary.add(countLabel("quadratic", quadCount, expectedQuad));
        for (java.util.Map.Entry<Integer, Integer> e : higherOrderCounts.entrySet()) {
            int degree = e.getKey();
            int expected = binomial(n, degree);
            summary.add(countLabel("degree-" + degree, e.getValue(), expected));
        }

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        legend.add(ViewFormatUtil.makeSwatch(ViewFormatUtil.termTypeColor(-1, -1), "constant"));
        legend.add(ViewFormatUtil.makeSwatch(ViewFormatUtil.termTypeColor(0, 0), "linear"));
        legend.add(ViewFormatUtil.makeSwatch(ViewFormatUtil.termTypeColor(0, 1), "quadratic"));
        if (!higherOrderCounts.isEmpty()) {
            legend.add(ViewFormatUtil.makeSwatch(ViewFormatUtil.termTypeColor(-2, -2), "degree-3+"));
        }

        JPanel header = new JPanel();
        header.setLayout(new javax.swing.BoxLayout(header, javax.swing.BoxLayout.Y_AXIS));
        header.add(summary);
        header.add(legend);
        header.add(search);

        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private static JLabel countLabel(String name, int actual, int expected) {
        JLabel l = ViewFormatUtil.statLabel(name + ": " + actual + "/" + expected);
        if (actual != expected) l.setForeground(Color.RED);
        return l;
    }

    /** n choose k, for the "expected probe count" summary at escalated degrees. */
    private static int binomial(int n, int k) {
        if (k < 0 || k > n) return 0;
        long result = 1;
        for (int i = 0; i < k; i++) {
            result = result * (n - i) / (i + 1);
        }
        return (int) result;
    }

    private static JTable buildTable(List<SampleRecord> samples, QuboResult result,
            MatrixTabPanel matrixTabPanel, Runnable switchToMatrixTab) {
        int n = samples.isEmpty() ? result.nVars : samples.get(0).vector.length;

        // Column headers: #, Phase, x0…xn-1, Value, Derived
        int colCount = 3 + n + 1;
        String[] colNames = new String[colCount];
        colNames[0] = "#";
        colNames[1] = "Phase";
        for (int i = 0; i < n; i++) colNames[2 + i] = "x" + i;
        colNames[2 + n] = "Value";
        colNames[3 + n] = "Derived";

        DefaultTableModel model = new DefaultTableModel(colNames, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        for (int k = 0; k < samples.size(); k++) {
            SampleRecord sr = samples.get(k);
            Object[] row = new Object[colCount];
            row[0] = k;
            row[1] = sr.phase;
            for (int i = 0; i < n; i++) row[2 + i] = sr.vector[i];
            row[2 + n] = String.format("%.4f", sr.rawValue);
            row[3 + n] = derivedLabel(sr);
            model.addRow(row);
        }

        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        table.getColumnModel().getColumn(0).setPreferredWidth(40);   // #
        table.getColumnModel().getColumn(1).setPreferredWidth(180);  // Phase
        for (int i = 0; i < n; i++) {
            table.getColumnModel().getColumn(2 + i).setPreferredWidth(20);
        }
        table.getColumnModel().getColumn(2 + n).setPreferredWidth(80);  // Value
        table.getColumnModel().getColumn(3 + n).setPreferredWidth(70);  // Derived

        int derivedCol = 3 + n;
        table.setDefaultRenderer(Object.class, new TableCellRenderer() {
            private final DefaultTableCellRenderer base = new DefaultTableCellRenderer();

            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = base.getTableCellRendererComponent(
                        t, value, isSelected, hasFocus, row, col);
                if (col >= 2 && col < 2 + n && value instanceof Integer) {
                    int bit = (Integer) value;
                    c.setBackground(bit == 1 ? new Color(80, 80, 80) : new Color(220, 220, 220));
                    c.setForeground(bit == 1 ? Color.WHITE : Color.DARK_GRAY);
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                    ((JLabel) c).setText(String.valueOf(bit));
                } else if (col == derivedCol) {
                    int modelRow = t.convertRowIndexToModel(row);
                    SampleRecord sr = samples.get(modelRow);
                    c.setBackground(isSelected ? t.getSelectionBackground()
                            : ViewFormatUtil.termTypeColor(sr.derivedI, sr.derivedJ));
                    c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(isSelected ? t.getSelectionBackground() : Color.WHITE);
                    c.setForeground(isSelected ? t.getSelectionForeground() : Color.BLACK);
                }
                return c;
            }
        });

        ToolTipManager.sharedInstance().registerComponent(table);
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) { table.setToolTipText(null); return; }
                int modelRow = table.convertRowIndexToModel(row);
                SampleRecord sr = samples.get(modelRow);
                Object[] rowData = new Object[colCount];
                for (int c = 0; c < colCount; c++) rowData[c] = model.getValueAt(modelRow, c);
                String base = ViewFormatUtil.buildVectorTooltip(rowData, n, result.varLabels);
                String withPrefix = base.replaceFirst("^<html>", "<html>" + termTypePrefix(sr, result));
                table.setToolTipText(withPrefix);
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() < 2) return;
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) return;
                int modelRow = table.convertRowIndexToModel(row);
                SampleRecord sr = samples.get(modelRow);
                // constant probe or degree-3+ probe: no single matrix cell to highlight
                if (sr.derivedI == -1 || sr.derivedI == -2) return;
                switchToMatrixTab.run();
                matrixTabPanel.highlightCell(sr.derivedI, sr.derivedJ);
            }
        });

        return table;
    }

    private static String termTypePrefix(SampleRecord sr, QuboResult result) {
        List<String> labels = result.varLabels;
        if (sr.derivedI == -1) return "Constant probe<br>";
        if (sr.derivedI == -2) {
            StringBuilder sb = new StringBuilder("Degree-").append(sr.termVars.length).append(" probe for ");
            for (int k = 0; k < sr.termVars.length; k++) {
                if (k > 0) sb.append(" × ");
                int v = sr.termVars[k];
                sb.append(v < labels.size() ? labels.get(v) : "x" + v);
            }
            return sb.append("<br>").toString();
        }
        if (sr.derivedI == sr.derivedJ) {
            String label = sr.derivedI < labels.size() ? labels.get(sr.derivedI) : "x" + sr.derivedI;
            return "Linear probe for " + label + "<br>";
        }
        String li = sr.derivedI < labels.size() ? labels.get(sr.derivedI) : "x" + sr.derivedI;
        String lj = sr.derivedJ < labels.size() ? labels.get(sr.derivedJ) : "x" + sr.derivedJ;
        return "Quadratic probe for " + li + " × " + lj + "<br>";
    }

    private static String derivedLabel(SampleRecord sr) {
        if (sr.derivedI == -1) return "c";
        if (sr.derivedI == -2) {
            StringBuilder sb = new StringBuilder("Q[");
            for (int k = 0; k < sr.termVars.length; k++) {
                if (k > 0) sb.append(",");
                sb.append(sr.termVars[k]);
            }
            return sb.append("]").toString();
        }
        if (sr.derivedI == sr.derivedJ) return "Q[" + sr.derivedI + "," + sr.derivedI + "]";
        return "Q[" + sr.derivedI + "," + sr.derivedJ + "]";
    }
}
