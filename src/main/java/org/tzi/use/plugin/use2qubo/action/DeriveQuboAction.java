package org.tzi.use.plugin.use2qubo.action;

import org.tzi.use.gui.main.MainWindow;
import org.tzi.use.gui.main.ViewFrame;
import org.tzi.use.main.Session;
import org.tzi.use.plugin.use2qubo.qubo.context.QuboContext;
import org.tzi.use.plugin.use2qubo.qubo.context.QuboContextBuilder;
import org.tzi.use.plugin.use2qubo.qubo.engine.QuboEngine;
import org.tzi.use.plugin.use2qubo.qubo.result.QuboResult;
import org.tzi.use.plugin.use2qubo.ui.QuboMatrixView;
import org.tzi.use.plugin.use2qubo.util.PluginLog;
import org.tzi.use.runtime.gui.IPluginAction;
import org.tzi.use.runtime.gui.IPluginActionDelegate;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * Menu action "Derive QUBO Matrix": builds a {@link QuboContext} from the loaded model/state,
 * runs {@link QuboEngine#derive} on a background {@link SwingWorker} (with a cancellable modal
 * progress dialog driven by the engine's step-label callback), and on success opens the result
 * in a {@link QuboMatrixView} docked inside a USE {@link ViewFrame}. Requires a loaded model with
 * non-empty state (a {@code .cmd} script already run).
 */
public class DeriveQuboAction implements IPluginActionDelegate {

    @Override
    public void performAction(IPluginAction pluginAction) {
        PluginLog.init(pluginAction.getParent().logWriter());
        PluginLog.info("Derive QUBO action triggered");
        Session session = pluginAction.getSession();
        MainWindow parent = pluginAction.getParent();

        if (!session.hasSystem()) {
            PluginLog.warn("Derive QUBO aborted: no model loaded");
            JOptionPane.showMessageDialog(parent,
                    "No model loaded. Open a .use file first.",
                    "Derive QUBO", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (session.system().state().allObjects().isEmpty()) {
            PluginLog.warn("Derive QUBO aborted: system state is empty");
            JOptionPane.showMessageDialog(parent,
                    "System state is empty. Run a .cmd script first.",
                    "Derive QUBO", JOptionPane.WARNING_MESSAGE);
            return;
        }

        QuboContext ctx;
        try {
            ctx = QuboContextBuilder.build(session.system());
            PluginLog.info("QUBO context built: nVars=" + ctx.nVars
                    + ", decisionVars=" + ctx.decisionVars.size()
                    + ", invariants=" + ctx.invariants.size());
        } catch (IOException e) {
            PluginLog.error("Failed to build QUBO context", e);
            JOptionPane.showMessageDialog(parent,
                    "Failed to build QUBO context:\n" + e.getMessage(),
                    "Derive QUBO", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Build progress dialog
        JLabel stepLabel = new JLabel("Initialising…");
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setPreferredSize(new Dimension(380, 22));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        body.add(new JLabel("Computing QUBO matrix — please wait."));
        body.add(Box.createVerticalStrut(10));
        body.add(bar);
        body.add(Box.createVerticalStrut(6));
        body.add(stepLabel);

        JButton cancelBtn = new JButton("Cancel");
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        btnPanel.add(cancelBtn);

        JDialog progressDialog = new JDialog(parent, "Deriving QUBO…", true);
        progressDialog.getContentPane().setLayout(new BorderLayout());
        progressDialog.getContentPane().add(body, BorderLayout.CENTER);
        progressDialog.getContentPane().add(btnPanel, BorderLayout.SOUTH);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(parent);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progressDialog.setResizable(false);

        SwingWorker<QuboResult, String> worker = new SwingWorker<QuboResult, String>() {
            @Override
            protected QuboResult doInBackground() throws Exception {
                PluginLog.info("QuboEngine.derive starting on background thread");
                long t0 = System.nanoTime();
                QuboResult result = QuboEngine.derive(ctx, this::publish,
                        (fromDegree, toDegree, expectedSamples) -> confirmEscalation(parent, fromDegree, toDegree, expectedSamples));
                long ms = (System.nanoTime() - t0) / 1_000_000;
                PluginLog.info("QuboEngine.derive finished in " + ms + " ms: " + result);
                return result.withDerivationMs(ms);
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    stepLabel.setText(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                try {
                    QuboResult result = get();
                    PluginLog.info("Derive QUBO complete, opening visualiser");
                    QuboMatrixView view = new QuboMatrixView(result);
                    ViewFrame frame = new ViewFrame("QUBO Matrix — " + result.nVars + " variables", view, null);
                    JComponent frameContent = (JComponent) frame.getContentPane();
                    frameContent.setLayout(new BorderLayout());
                    frameContent.add(view, BorderLayout.CENTER);
                    MainWindow.instance().addNewViewFrame(frame);
                    frame.setVisible(true);
                } catch (CancellationException e) {
                    PluginLog.info("Derive QUBO cancelled by user");
                } catch (ExecutionException e) {
                    PluginLog.error("QUBO derivation failed", e.getCause());
                    JOptionPane.showMessageDialog(parent,
                            "Derivation failed:\n" + e.getCause().getMessage(),
                            "Derive QUBO", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    PluginLog.warn("Derive QUBO interrupted", e);
                }
            }
        };

        cancelBtn.addActionListener(e -> worker.cancel(true));
        worker.execute();
        progressDialog.setVisible(true); // blocks EDT until done() disposes the dialog
    }

    @Override
    public boolean shouldBeEnabled(IPluginAction pluginAction) {
        return pluginAction.getSession().hasSystem();
    }

    /**
     * Shows a Yes/No confirmation dialog on the EDT before sampling escalates to a higher
     * degree, since {@link QuboEngine#derive} runs on a background {@link SwingWorker} thread.
     * On failure to show the dialog, declines (safe default: stop rather than hang).
     */
    private static boolean confirmEscalation(MainWindow parent, int fromDegree, int toDegree, long expectedSamples) {
        final boolean[] proceed = {false};
        try {
            SwingUtilities.invokeAndWait(() -> {
                int result = JOptionPane.showConfirmDialog(parent,
                        "Escalating to degree " + toDegree + " will run " + expectedSamples
                                + " additional live-model evaluations. Continue?",
                        "Degree Escalation", JOptionPane.YES_NO_OPTION);
                proceed[0] = result == JOptionPane.YES_OPTION;
            });
        } catch (InvocationTargetException | InterruptedException e) {
            PluginLog.warn("Failed to show degree-escalation confirmation dialog; declining", e);
            return false;
        }
        return proceed[0];
    }
}
