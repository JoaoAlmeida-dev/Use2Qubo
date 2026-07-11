package org.tzi.use.plugin.use2qubo.ui.tabs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ToolTipManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.tzi.use.plugin.use2qubo.qubo.result.ExactnessPoint;
import org.tzi.use.plugin.use2qubo.qubo.result.QuboResult;
import org.tzi.use.plugin.use2qubo.ui.ViewFormatUtil;

/** "Exactness" tab: held-out-vector exactness-check table (true f(x) vs QUBO approx q(x)). */
public class ExactnessTabPanel extends JPanel {

    public ExactnessTabPanel(QuboResult result) {
        super(new BorderLayout());
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

        setBorder(BorderFactory.createTitledBorder("Exactness check ("
                + points.size() + " " + result.exactnessMethod + " points shown)"));

        JLabel summary = new JLabel(buildSummaryText(result));
        summary.setFont(summary.getFont().deriveFont(Font.BOLD));
        summary.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        summary.setToolTipText(result.exactnessMatchCount + "/" + result.exactnessTotalCount + " exact samples");

        add(summary, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /** e.g. "exhaustive verification: 100% (261/261)" or "sampled verification: 77% (154/200)". */
    private static String buildSummaryText(QuboResult result) {
        int total = result.exactnessTotalCount;
        double pct = total > 0 ? 100.0 * result.exactnessMatchCount / total : 0.0;
        return String.format(Locale.ROOT, "%s verification: %.0f%% (%d/%d)",
                result.exactnessMethod, pct, result.exactnessMatchCount, total);
    }
}
