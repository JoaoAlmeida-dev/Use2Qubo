package org.tzi.use.plugin.use2qubo.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.tzi.use.plugin.use2qubo.qubo.ExactnessPoint;
import org.tzi.use.plugin.use2qubo.qubo.QuboResult;
import org.tzi.use.plugin.use2qubo.qubo.SampleRecord;

/** "Sampling" tab: cost/penalty sample heatmap above, exactness-check table below. */
class SamplingTabPanel extends JSplitPane {

    SamplingTabPanel(QuboResult result) {
        super(JSplitPane.VERTICAL_SPLIT, buildHeatmapPanel(result), buildExactnessPanel(result));
        setResizeWeight(0.6);
    }

    private static JScrollPane buildHeatmapPanel(QuboResult result) {
        int n = result.nVars;

        List<SampleRecord> cost    = result.costSamples;
        List<SampleRecord> penalty = result.penaltySamples;

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

        for (int k = 0; k < cost.size(); k++) {
            SampleRecord sr = cost.get(k);
            Object[] row = new Object[colCount];
            row[0] = k;
            row[1] = sr.phase;
            for (int i = 0; i < n; i++) row[2 + i] = sr.vector[i];
            row[2 + n] = String.format("%.4f", sr.rawValue);
            row[3 + n] = derivedLabel(sr.derivedI, sr.derivedJ);
            model.addRow(row);
        }

        Object[] divider = new Object[colCount];
        divider[0] = "—";
        divider[1] = "── penalty pass ──";
        for (int i = 0; i < n; i++) divider[2 + i] = null;
        divider[2 + n] = null;
        divider[3 + n] = null;
        model.addRow(divider);

        for (int k = 0; k < penalty.size(); k++) {
            SampleRecord sr = penalty.get(k);
            Object[] row = new Object[colCount];
            row[0] = cost.size() + k;
            row[1] = sr.phase;
            for (int i = 0; i < n; i++) row[2 + i] = sr.vector[i];
            row[2 + n] = String.format("%.4f", sr.rawValue);
            row[3 + n] = derivedLabel(sr.derivedI, sr.derivedJ);
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

        int dividerRow = cost.size();
        table.setDefaultRenderer(Object.class, new TableCellRenderer() {
            private final DefaultTableCellRenderer base = new DefaultTableCellRenderer();

            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = base.getTableCellRendererComponent(
                        t, value, isSelected, hasFocus, row, col);
                if (row == dividerRow) {
                    c.setBackground(new Color(220, 220, 180));
                    c.setForeground(Color.DARK_GRAY);
                } else if (col >= 2 && col < 2 + n && value instanceof Integer) {
                    int bit = (Integer) value;
                    c.setBackground(bit == 1 ? new Color(80, 80, 80) : new Color(220, 220, 220));
                    c.setForeground(bit == 1 ? Color.WHITE : Color.DARK_GRAY);
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                    ((JLabel) c).setText(String.valueOf(bit));
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
                if (row < 0 || row == dividerRow) { table.setToolTipText(null); return; }
                Object[] rowData = new Object[colCount];
                for (int c = 0; c < colCount; c++) rowData[c] = model.getValueAt(row, c);
                table.setToolTipText(ViewFormatUtil.buildVectorTooltip(rowData, n, result.varLabels));
            }
        });

        return new JScrollPane(table);
    }

    private static JPanel buildExactnessPanel(QuboResult result) {
        List<ExactnessPoint> points = result.exactnessPoints;

        String[] cols = {"#", "x (Hamming wt)", "f(x)", "q(x)", "|error|", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        int worstRow = -1;
        double worstError = -1.0;
        for (int k = 0; k < points.size(); k++) {
            ExactnessPoint pt = points.get(k);
            if (!pt.evalFailed) {
                double err = pt.error();
                if (err > worstError) { worstError = err; worstRow = k; }
            }
        }

        for (int k = 0; k < points.size(); k++) {
            ExactnessPoint pt = points.get(k);
            int hw = 0;
            for (int v : pt.vector) hw += v;
            if (pt.evalFailed) {
                model.addRow(new Object[]{k, "wt=" + hw, "—", "—", "—", "ERR"});
            } else {
                double err = pt.error();
                String status = err < 1e-9 ? "✓" : "✗";
                model.addRow(new Object[]{
                        k,
                        "wt=" + hw,
                        String.format("%.6f", pt.fx),
                        String.format("%.6f", pt.qx),
                        String.format("%.2e", err),
                        status
                });
            }
        }

        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(5).setPreferredWidth(55);

        final int capturedWorstRow = worstRow;
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(
                        t, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row == capturedWorstRow ? new Color(255, 200, 200) : Color.WHITE);
                }
                return c;
            }
        });

        ToolTipManager.sharedInstance().registerComponent(table);
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0 || row >= points.size()) { table.setToolTipText(null); return; }
                ExactnessPoint pt = points.get(row);
                table.setToolTipText(ViewFormatUtil.buildVectorAssignment(pt.vector, result.varLabels));
            }
        });

        Color headerBg = result.exact ? new Color(0, 150, 0) : new Color(200, 0, 0);
        table.getTableHeader().setBackground(headerBg);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Exactness check ("
                + points.size() + " held-out vectors)"));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private static String derivedLabel(int i, int j) {
        if (i == -1) return "c";
        if (i == j)  return "Q[" + i + "," + i + "]";
        return "Q[" + i + "," + j + "]";
    }
}
