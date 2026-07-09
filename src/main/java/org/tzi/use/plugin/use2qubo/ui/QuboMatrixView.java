package org.tzi.use.plugin.use2qubo.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.tzi.use.gui.views.View;
import org.tzi.use.plugin.use2qubo.qubo.context.QuboContext;
import org.tzi.use.plugin.use2qubo.qubo.result.QuboResult;
import org.tzi.use.plugin.use2qubo.qubo.result.QuboResultExporter;
import org.tzi.use.plugin.use2qubo.ui.tabs.ExactnessTabPanel;
import org.tzi.use.plugin.use2qubo.ui.tabs.MatrixTabPanel;
import org.tzi.use.plugin.use2qubo.ui.tabs.QuboGraphPanel;
import org.tzi.use.plugin.use2qubo.ui.tabs.SamplingTabPanel;
import org.tzi.use.plugin.use2qubo.ui.tabs.TermsTabPanel;
import org.tzi.use.plugin.use2qubo.ui.tabs.TryItTabPanel;
import org.tzi.use.plugin.use2qubo.util.PluginLog;

/**
 * Top-level dockable view for a {@link QuboResult}: a summary stats header plus a tabbed pane
 * (matrix, terms, sampling, exactness, graph — see {@code ui.tabs}) and an export-to-JSON
 * button ({@link QuboResultExporter}). Opened by {@link
 * org.tzi.use.plugin.use2qubo.action.DeriveQuboAction} after a successful derivation.
 */
public class QuboMatrixView extends JPanel implements View {

    public QuboMatrixView(QuboResult result, QuboContext ctx) {
        setLayout(new BorderLayout(4, 4));
        add(buildStatsPanel(result), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        MatrixTabPanel matrixTab = new MatrixTabPanel(result);
        tabs.addTab("Matrix", matrixTab);
        tabs.addTab("Terms", new TermsTabPanel(result));
        tabs.addTab("Graph", new QuboGraphPanel(result));
        tabs.addTab("Sampling", new SamplingTabPanel(result, matrixTab, () -> tabs.setSelectedComponent(matrixTab)));
        tabs.addTab("Exactness", new ExactnessTabPanel(result));
        tabs.addTab("Try It", new TryItTabPanel(result, ctx));
        add(tabs, BorderLayout.CENTER);

        add(buildButtonPanel(result), BorderLayout.SOUTH);

        int width = Math.min(result.nVars * 80 + 260, 1150);
        setPreferredSize(new Dimension(width, 620));
    }

    private JPanel buildStatsPanel(QuboResult result) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        panel.setBorder(BorderFactory.createEtchedBorder());

        panel.add(ViewFormatUtil.statLabel("nVars = " + result.nVars));
        panel.add(ViewFormatUtil.statLabel("nSamples = " + result.nSamples));
        panel.add(ViewFormatUtil.statLabel(String.format("constant = %.3f", result.constant)));

        int nnz = result.linear.size() + result.quadratic.size();
        long n2 = (long) result.nVars * result.nVars;
        double density = n2 > 0 ? 100.0 * nnz / n2 : 0.0;
        panel.add(ViewFormatUtil.statLabel("nnz = " + nnz));
        panel.add(ViewFormatUtil.statLabel(String.format("density = %.1f%%", density)));
        panel.add(ViewFormatUtil.statLabel(String.format("B = %.1f", result.penaltyWeight)));
        panel.add(ViewFormatUtil.statLabel("time = " + result.derivationMs + "ms"));

        panel.add(new JLabel("exact:"));
        JLabel exactBadge = new JLabel(result.exact ? "YES" : "NO");
        exactBadge.setOpaque(true);
        exactBadge.setForeground(Color.WHITE);
        exactBadge.setBackground(result.exact ? new Color(0, 150, 0) : new Color(200, 0, 0));
        exactBadge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        panel.add(exactBadge);

        panel.add(Box.createHorizontalStrut(8));
        panel.add(ViewFormatUtil.makeSwatch(new Color(205, 92, 92), "negative"));
        panel.add(ViewFormatUtil.makeSwatch(new Color(100, 149, 237), "positive"));

        return panel;
    }

    private JPanel buildButtonPanel(QuboResult result) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));

        JButton save = new JButton("Save to File");
        save.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Export QUBO Matrix to JSON");
            chooser.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));
            chooser.setSelectedFile(new File("qubo_matrix.json"));
            if (chooser.showSaveDialog(QuboMatrixView.this) != JFileChooser.APPROVE_OPTION) return;
            File chosen = chooser.getSelectedFile();
            if (!chosen.getName().endsWith(".json"))
                chosen = new File(chosen.getParentFile(), chosen.getName() + ".json");
            final File outputFile = chosen;

            save.setEnabled(false);
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws IOException {
                    QuboResultExporter.write(result, outputFile);
                    return null;
                }
                @Override
                protected void done() {
                    save.setEnabled(true);
                    try {
                        get();
                        PluginLog.info("QUBO saved to " + outputFile.getAbsolutePath());
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        PluginLog.error("Save failed", cause);
                        JOptionPane.showMessageDialog(QuboMatrixView.this,
                                "Failed to save:\n" + cause.getMessage(),
                                "Save Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });

        JButton close = new JButton("Close");
        close.addActionListener(e ->
                ((JInternalFrame) SwingUtilities.getAncestorOfClass(JInternalFrame.class, QuboMatrixView.this)).dispose());

        panel.add(save);
        panel.add(close);
        return panel;
    }

    @Override
    public void detachModel() {}
}
