package org.tzi.use.plugin.use2qubo.ui.tabs;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.tzi.use.plugin.use2qubo.qubo.result.QuboResult;

/** "Terms" tab: flat list of all non-zero coefficients, sorted by |coefficient| descending. */
public class TermsTabPanel extends JScrollPane {

    public TermsTabPanel(QuboResult result) {
        String[] cols = {"Type", "i", "j", "Label(s)", "Coefficient"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"constant", "—", "—", "—", result.constant});

        for (Map.Entry<Integer, Double> e : result.linear.entrySet()) {
            int i = e.getKey();
            String label = i < result.varLabels.size() ? result.varLabels.get(i) : String.valueOf(i);
            rows.add(new Object[]{"linear", i, "—", label, e.getValue()});
        }

        for (Map.Entry<String, Double> e : result.quadratic.entrySet()) {
            String[] parts = e.getKey().split(",");
            int i = Integer.parseInt(parts[0]);
            int j = Integer.parseInt(parts[1]);
            String li = i < result.varLabels.size() ? result.varLabels.get(i) : String.valueOf(i);
            String lj = j < result.varLabels.size() ? result.varLabels.get(j) : String.valueOf(j);
            rows.add(new Object[]{"quadratic", i, j, li + " × " + lj, e.getValue()});
        }

        rows.sort((a, b) -> Double.compare(Math.abs((Double) b[4]), Math.abs((Double) a[4])));

        for (Object[] row : rows) {
            model.addRow(row);
        }

        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(40);
        table.getColumnModel().getColumn(2).setPreferredWidth(40);
        table.getColumnModel().getColumn(3).setPreferredWidth(300);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            {
                setHorizontalAlignment(RIGHT);
            }
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (v instanceof Double) setText(String.format("%.6f", (Double) v));
                return this;
            }
        });

        setViewportView(table);
    }
}
