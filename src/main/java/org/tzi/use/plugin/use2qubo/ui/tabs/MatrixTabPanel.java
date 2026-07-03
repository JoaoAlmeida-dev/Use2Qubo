package org.tzi.use.plugin.use2qubo.ui.tabs;

import org.tzi.use.plugin.use2qubo.qubo.QuboResult;
import org.tzi.use.plugin.use2qubo.ui.ExpressionPanel;
import org.tzi.use.plugin.use2qubo.ui.ViewFormatUtil;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;

/** "Matrix" tab: colour-coded Q-matrix table, row/column variable index, and the algebraic expression below it. */
public class MatrixTabPanel extends JSplitPane {

    private final JTable matrixTable;
    private final JTable idxTable;

    public MatrixTabPanel(QuboResult result) {
        super(JSplitPane.VERTICAL_SPLIT);
        int n = result.nVars;

        double maxAbs = 0.0;
        for (double v : result.linear.values())    maxAbs = Math.max(maxAbs, Math.abs(v));
        for (double v : result.quadratic.values()) maxAbs = Math.max(maxAbs, Math.abs(v));

        String[] colNames = new String[n];
        for (int i = 0; i < n; i++) {
            colNames[i] = ViewFormatUtil.abbrev(result.varLabels.get(i));
        }

        Double[][] data = new Double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    data[i][j] = result.linear.getOrDefault(i, 0.0);
                } else if (i < j) {
                    data[i][j] = result.quadratic.getOrDefault(i + "," + j, 0.0);
                } else {
                    data[i][j] = result.quadratic.getOrDefault(j + "," + i, 0.0);
                }
            }
        }

        DefaultTableModel model = new DefaultTableModel(n, n) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) { return Double.class; }
        };
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                model.setValueAt(data[i][j], i, j);
            }
        }

        final double capturedMaxAbs = maxAbs;
        JTable table = new JTable(model) {
            @Override
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    @Override
                    public String getToolTipText(MouseEvent e) {
                        int col = columnAtPoint(e.getPoint());
                        if (col < 0 || col >= result.varLabels.size()) return null;
                        return result.varLabels.get(col);
                    }
                };
            }
        };
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setCellSelectionEnabled(true);
        table.setDefaultRenderer(Double.class, new ColoredCellRenderer(capturedMaxAbs));
        this.matrixTable = table;

        for (int c = 0; c < n; c++) {
            table.getColumnModel().getColumn(c).setHeaderValue(colNames[c]);
            table.getColumnModel().getColumn(c).setPreferredWidth(80);
        }

        JList<String> rowHeader = new JList<String>(colNames) {
            @Override
            public String getToolTipText(MouseEvent e) {
                int idx = locationToIndex(e.getPoint());
                if (idx < 0 || idx >= result.varLabels.size()) return null;
                return result.varLabels.get(idx);
            }
        };
        ToolTipManager.sharedInstance().registerComponent(rowHeader);
        rowHeader.setFixedCellWidth(100);
        rowHeader.setFixedCellHeight(table.getRowHeight());
        rowHeader.setCellRenderer(new RowHeaderRenderer(table));

        JScrollPane matrixScroll = new JScrollPane(table);
        matrixScroll.setRowHeaderView(rowHeader);
        matrixScroll.setCorner(JScrollPane.UPPER_LEFT_CORNER, new JLabel("var \\ var"));

        // Variable index side panel
        DefaultTableModel idxModel = new DefaultTableModel(new String[]{"Idx", "Variable"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (int i = 0; i < n; i++) {
            idxModel.addRow(new Object[]{i, result.varLabels.get(i)});
        }
        JTable idxTable = new JTable(idxModel);
        idxTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        idxTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        this.idxTable = idxTable;
        JScrollPane idxScroll = new JScrollPane(idxTable);
        idxScroll.setPreferredSize(new Dimension(250, 0));

        javax.swing.event.ListSelectionListener syncListener = e -> {
            if (e.getValueIsAdjusting()) return;
            highlightVariablesForCell(table.getSelectedRow(), table.getSelectedColumn());
        };
        table.getSelectionModel().addListSelectionListener(syncListener);
        table.getColumnModel().getSelectionModel().addListSelectionListener(syncListener);

        JSplitPane hSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, matrixScroll, idxScroll);
        hSplit.setResizeWeight(0.75);

        setTopComponent(hSplit);
        setBottomComponent(new ExpressionPanel(result));
        setResizeWeight(0.7);
    }

    /** Selects and scrolls to the Q-matrix cell at (i, j); called from the Sampling tab. */
    public void highlightCell(int i, int j) {
        if (i < 0 || j < 0 || i >= matrixTable.getRowCount() || j >= matrixTable.getColumnCount()) return;
        matrixTable.requestFocusInWindow();
        matrixTable.changeSelection(i, j, false, false);
        matrixTable.scrollRectToVisible(matrixTable.getCellRect(i, j, true));
    }

    /** Highlights the variable-index row(s) for the selected Q-matrix cell (i, j) in the variable table. */
    private void highlightVariablesForCell(int i, int j) {
        if (i < 0 || j < 0 || i >= idxTable.getRowCount() || j >= idxTable.getRowCount()) return;
        idxTable.getSelectionModel().setValueIsAdjusting(true);
        idxTable.getSelectionModel().clearSelection();
        idxTable.getSelectionModel().addSelectionInterval(i, i);
        if (j != i) idxTable.getSelectionModel().addSelectionInterval(j, j);
        idxTable.getSelectionModel().setValueIsAdjusting(false);
        idxTable.scrollRectToVisible(idxTable.getCellRect(i, 0, true));
    }

    private static final class ColoredCellRenderer extends JLabel implements TableCellRenderer {
        private static final Border SELECTED_BORDER = BorderFactory.createLineBorder(Color.ORANGE, 3);
        private static final Border UNSELECTED_BORDER = BorderFactory.createEmptyBorder(3, 3, 3, 3);

        private final double maxAbs;

        ColoredCellRenderer(double maxAbs) {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
            this.maxAbs = maxAbs;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            double v = (value instanceof Double) ? (Double) value : 0.0;
            setText(String.format("%.3f", v));
            setBackground(ViewFormatUtil.colorForValue(v, maxAbs));
            setBorder(isSelected ? SELECTED_BORDER : UNSELECTED_BORDER);
            return this;
        }
    }

    private static final class RowHeaderRenderer extends JLabel implements ListCellRenderer<String> {
        RowHeaderRenderer(JTable table) {
            setOpaque(true);
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setHorizontalAlignment(CENTER);
            setFont(table.getTableHeader().getFont());
            setBackground(table.getTableHeader().getBackground());
            setForeground(table.getTableHeader().getForeground());
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list,
                String value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value);
            return this;
        }
    }
}
