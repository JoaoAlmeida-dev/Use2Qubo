package org.tzi.use.plugin.use2qubo.ui.tabs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;

import org.tzi.use.gui.main.MainWindow;
import org.tzi.use.gui.views.diagrams.objectdiagram.NewObjectDiagramView;
import org.tzi.use.gui.views.diagrams.objectdiagram.ObjDiagramOptions;
import org.tzi.use.plugin.use2qubo.qubo.context.DecisionVar;
import org.tzi.use.plugin.use2qubo.qubo.context.QuboContext;
import org.tzi.use.plugin.use2qubo.qubo.context.SandboxSystemFactory;
import org.tzi.use.plugin.use2qubo.qubo.engine.QuboEngine;
import org.tzi.use.plugin.use2qubo.qubo.result.QuboResult;
import org.tzi.use.plugin.use2qubo.ui.ViewFormatUtil;
import org.tzi.use.plugin.use2qubo.util.PluginLog;
import org.tzi.use.plugin.use2qubo.util.QuboConstants;
import org.tzi.use.uml.mm.MAssociation;
import org.tzi.use.uml.sys.MObject;

/**
 * "Try It" tab: lets a user toggle an arbitrary binary assignment over the original decision
 * variables, then compares q(x) (derived QUBO polynomial) against the true OCL objective/penalty
 * evaluated live on {@code ctx}'s own sandbox ({@link QuboEngine#evaluateTrue}), with a
 * structure-only object-diagram preview of the resulting link configuration.
 */
public class TryItTabPanel extends JSplitPane {

    private static final double EPS = QuboConstants.EPS;

    private final QuboResult result;
    private final QuboContext ctx;
    private final List<Entry> entries;
    private final JCheckBox[] checkBoxes;
    private final JLabel[] ancillaLabels;
    private final JLabel costLabel;
    private final JLabel penaltyLabel;
    private final JLabel qLabel;
    private final JLabel weightedLabel;
    private final JLabel badge;
    private final JLabel noteLabel;
    private final JButton evaluateButton;
    private final JPanel diagramContainer;
    private NewObjectDiagramView currentDiagramView;

    public TryItTabPanel(QuboResult result, QuboContext ctx) {
        super(JSplitPane.HORIZONTAL_SPLIT);
        this.result = result;
        this.ctx = ctx;
        this.entries = buildEntries(ctx);
        this.checkBoxes = new JCheckBox[ctx.nVars];
        this.ancillaLabels = new JLabel[result.nAncillaVars];

        JPanel varsPanel = new JPanel();
        varsPanel.setLayout(new BoxLayout(varsPanel, BoxLayout.Y_AXIS));
        for (int i = 0; i < ctx.nVars; i++) {
            JCheckBox cb = new JCheckBox(result.varLabels.get(i));
            checkBoxes[i] = cb;
            varsPanel.add(cb);
        }

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton randomize = new JButton("Randomize");
        JButton resetZero = new JButton("Reset to 0");
        JButton resetOne = new JButton("Reset to 1");
        randomize.addActionListener(e -> {
            Random rand = new Random();
            for (JCheckBox cb : checkBoxes) cb.setSelected(rand.nextBoolean());
            refreshAncillaDisplay();
        });
        resetZero.addActionListener(e -> {
            for (JCheckBox cb : checkBoxes) cb.setSelected(false);
            refreshAncillaDisplay();
        });
        resetOne.addActionListener(e -> {
            for (JCheckBox cb : checkBoxes) cb.setSelected(true);
            refreshAncillaDisplay();
        });
        toolbar.add(randomize);
        toolbar.add(resetZero);
        toolbar.add(resetOne);

        for (JCheckBox cb : checkBoxes) {
            cb.addItemListener(e -> refreshAncillaDisplay());
        }

        JPanel ancillaPanel = new JPanel();
        ancillaPanel.setLayout(new BoxLayout(ancillaPanel, BoxLayout.Y_AXIS));
        if (result.nAncillaVars > 0) {
            ancillaPanel.setBorder(BorderFactory.createTitledBorder("Derived ancilla bits"));
            for (int k = 0; k < result.nAncillaVars; k++) {
                JLabel lbl = new JLabel();
                ancillaLabels[k] = lbl;
                ancillaPanel.add(lbl);
            }
        }

        evaluateButton = new JButton("Evaluate");
        JPanel evalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        evalPanel.add(evaluateButton);

        costLabel = ViewFormatUtil.statLabel("f_cost(x) = —");
        penaltyLabel = ViewFormatUtil.statLabel("penalty(x) = —");
        qLabel = ViewFormatUtil.statLabel("q(x) = —");
        weightedLabel = ViewFormatUtil.statLabel("f_cost(x) + B*penalty(x) = —");
        badge = new JLabel("—");
        badge.setOpaque(true);
        badge.setForeground(Color.WHITE);
        badge.setBackground(Color.GRAY);
        badge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        noteLabel = new JLabel(" ");

        JPanel resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Result"));
        resultsPanel.add(qLabel);
        resultsPanel.add(costLabel);
        resultsPanel.add(penaltyLabel);
        resultsPanel.add(weightedLabel);
        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        badgeRow.add(new JLabel("match:"));
        badgeRow.add(badge);
        resultsPanel.add(badgeRow);
        resultsPanel.add(noteLabel);

        JPanel leftPanel = new JPanel(new BorderLayout(4, 4));
        leftPanel.add(toolbar, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(varsPanel), BorderLayout.CENTER);

        JPanel bottomLeft = new JPanel();
        bottomLeft.setLayout(new BoxLayout(bottomLeft, BoxLayout.Y_AXIS));
        bottomLeft.add(ancillaPanel);
        bottomLeft.add(evalPanel);
        bottomLeft.add(resultsPanel);
        leftPanel.add(bottomLeft, BorderLayout.SOUTH);

        diagramContainer = new JPanel(new BorderLayout());
        diagramContainer.setBorder(BorderFactory.createTitledBorder("Sandbox preview (structure only)"));

        setLeftComponent(leftPanel);
        setRightComponent(new JScrollPane(diagramContainer));
        setResizeWeight(0.45);
        setPreferredSize(new Dimension(900, 500));

        refreshAncillaDisplay();
        evaluateButton.addActionListener(e -> runEvaluate());
    }

    private int[] currentOriginalVector() {
        int[] x = new int[ctx.nVars];
        for (int i = 0; i < ctx.nVars; i++) x[i] = checkBoxes[i].isSelected() ? 1 : 0;
        return x;
    }

    private int[] currentFullVector() {
        int[] full = new int[result.nVars];
        int[] original = currentOriginalVector();
        System.arraycopy(original, 0, full, 0, ctx.nVars);
        for (int k = 0; k < result.nAncillaVars; k++) {
            int[] pair = result.ancillaPairs.get(k);
            full[ctx.nVars + k] = full[pair[0]] * full[pair[1]];
        }
        return full;
    }

    private void refreshAncillaDisplay() {
        if (result.nAncillaVars == 0) return;
        int[] full = currentFullVector();
        for (int k = 0; k < result.nAncillaVars; k++) {
            String label = ctx.nVars + k < result.varLabels.size()
                    ? result.varLabels.get(ctx.nVars + k) : "anc" + k;
            ancillaLabels[k].setText(label + " = " + full[ctx.nVars + k]);
        }
    }

    private void runEvaluate() {
        evaluateButton.setEnabled(false);
        for (JCheckBox cb : checkBoxes) cb.setEnabled(false);

        int[] originalX = currentOriginalVector();
        int[] fullX = currentFullVector();

        new SwingWorker<QuboEngine.TrueEval, Void>() {
            @Override
            protected QuboEngine.TrueEval doInBackground() throws Exception {
                return QuboEngine.evaluateTrue(ctx, originalX);
            }

            @Override
            protected void done() {
                evaluateButton.setEnabled(true);
                for (JCheckBox cb : checkBoxes) cb.setEnabled(true);
                try {
                    QuboEngine.TrueEval trueEval = get();
                    showResult(fullX, trueEval);
                    updateDiagram(originalX);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    PluginLog.error("Try It: evaluation failed", cause);
                    JOptionPane.showMessageDialog(TryItTabPanel.this,
                            "Evaluation failed:\n" + cause.getMessage(),
                            "Try It", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void showResult(int[] fullX, QuboEngine.TrueEval trueEval) {
        double qx = result.eval(fullX);
        double weighted = trueEval.weighted(result.penaltyWeight);

        qLabel.setText(String.format("q(x) = %.4f", qx));
        costLabel.setText(String.format("f_cost(x) = %.4f", trueEval.cost));
        penaltyLabel.setText(String.format("penalty(x) = %.0f violation(s)", trueEval.penalty));
        weightedLabel.setText(String.format("f_cost(x) + B*penalty(x) = %.4f", weighted));

        boolean match = Math.abs(qx - weighted) < EPS;
        badge.setText(match ? "MATCH" : "MISMATCH");
        badge.setBackground(match ? new Color(0, 150, 0) : new Color(200, 0, 0));

        if (!match) {
            noteLabel.setText("<html>q(x) and f(x) disagree on this vector. This is consistent with "
                    + "result.exact=" + result.exact + " (see the Exactness tab) — common causes are boolean "
                    + "pass/fail invariants or interactions beyond the sampled polynomial degree.</html>");
        } else {
            noteLabel.setText(" ");
        }
    }

    private void updateDiagram(int[] originalX) {
        try {
            SandboxSystemFactory.Sandbox sandbox = SandboxSystemFactory.build(
                    ctx.model, ctx.state, ctx.objectsByClass, false, ctx.fixedLinks);

            for (int i = 0; i < entries.size(); i++) {
                if (originalX[i] == 0) continue;
                Entry entry = entries.get(i);
                MAssociation assoc = ctx.model.getAssociation(entry.association);
                if (assoc == null) continue;
                MObject a = sandbox.byName.get(entry.a.name());
                MObject b = sandbox.byName.get(entry.b.name());
                if (a == null || b == null) continue;
                sandbox.state.createLink(assoc, Arrays.asList(a, b), Collections.emptyList());
            }

            NewObjectDiagramView newView = new NewObjectDiagramView(MainWindow.instance(), sandbox.system);
            ObjDiagramOptions options = new ObjDiagramOptions();
            options.setShowAttributes(false);
            newView.initDiagram(true, options);

            if (currentDiagramView != null) {
                currentDiagramView.detachModel();
            }
            diagramContainer.removeAll();
            diagramContainer.add(newView, BorderLayout.CENTER);
            diagramContainer.revalidate();
            diagramContainer.repaint();
            currentDiagramView = newView;
        } catch (Exception e) {
            PluginLog.error("Try It: failed to build sandbox preview diagram", e);
            JOptionPane.showMessageDialog(this,
                    "Failed to build preview diagram:\n" + e.getMessage(),
                    "Try It", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------------------------

    /** One flat (association, a, b) entry per original decision variable, in the exact order
     *  {@link QuboContext#varIndex} and {@link QuboEngine}'s internal flat variable list use:
     *  decision_vars list order, then (a,b) pairs sorted lexicographically by name. */
    private static final class Entry {
        final String association;
        final MObject a;
        final MObject b;

        Entry(String association, MObject a, MObject b) {
            this.association = association;
            this.a = a;
            this.b = b;
        }
    }

    private static List<Entry> buildEntries(QuboContext ctx) {
        List<Entry> flat = new ArrayList<>(ctx.nVars);
        for (DecisionVar dv : ctx.decisionVars) {
            List<MObject> bObjs = ctx.objectsByClass.getOrDefault(dv.classB, Collections.emptyList());
            List<MObject[]> pairs = new ArrayList<>(dv.domain.size() * bObjs.size());
            for (MObject a : dv.domain) {
                for (MObject b : bObjs) {
                    pairs.add(new MObject[]{a, b});
                }
            }
            pairs.sort(Comparator.<MObject[], String>comparing(p -> p[0].name())
                                  .thenComparing(p -> p[1].name()));
            for (MObject[] pair : pairs) {
                flat.add(new Entry(dv.association, pair[0], pair[1]));
            }
        }
        return flat;
    }
}
