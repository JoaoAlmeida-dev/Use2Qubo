package org.tzi.use.plugin.use2qubo.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.tzi.use.gui.views.View;
import org.tzi.use.parser.ocl.OCLCompiler;
import org.tzi.use.plugin.use2qubo.qubo.QuboConfig;
import org.tzi.use.plugin.use2qubo.util.PluginLog;
import org.tzi.use.plugin.use2qubo.util.QuboConstants;
import org.tzi.use.plugin.use2qubo.util.SimpleJsonWriter;
import org.tzi.use.uml.mm.MAssociation;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.ocl.expr.Expression;
import org.tzi.use.uml.ocl.value.VarBindings;

public class QuboConfigView extends JPanel implements View {

    private final JTextField objectiveField = new JTextField(50);
    private final JCheckBox minimiseBox = new JCheckBox("Minimise", true);
    private final JSpinner maxDegreeSpinner =
            new JSpinner(new SpinnerNumberModel(QuboConstants.DEFAULT_MAX_POLY_DEGREE, 2, 6, 1));
    private final Map<String, JCheckBox> assocCheckboxes = new LinkedHashMap<>();
    private final MModel model;

    public QuboConfigView(File configFile, MModel model) {
        this.model = model;
        setLayout(new BorderLayout(4, 4));
        add(buildFilePathPanel(configFile), BorderLayout.NORTH);
        add(buildFormPanel(), BorderLayout.CENTER);
        add(buildButtonPanel(configFile), BorderLayout.SOUTH);
        prefill(configFile);
    }

    private JPanel buildFilePathPanel(File configFile) {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 8, 2, 8));
        JTextField pathField = new JTextField(configFile.getAbsolutePath());
        pathField.setEditable(false);
        pathField.setBackground(UIManager.getColor("Panel.background"));
        pathField.setFont(pathField.getFont().deriveFont(Font.PLAIN, 11f));
        panel.add(new JLabel("Config file: "), BorderLayout.WEST);
        panel.add(pathField, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildFormPanel() {
        JLabel validateStatus = new JLabel(" ");

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(4, 4, 4, 8);
        GridBagConstraints fc = new GridBagConstraints();
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets = new Insets(4, 0, 4, 4);

        objectiveField.setToolTipText(
                "OCL expression evaluated over the system state. "
                + "Example: Truck.allInstances()->collect(t | t.load)->sum()");

        JButton validateBtn = new JButton("Validate OCL");
        validateBtn.addActionListener(e -> validateOcl(validateStatus));

        // Row 0: label + [objectiveField | validateBtn]
        int row = 0;
        lc.gridy = row; lc.gridx = 0;
        panel.add(new JLabel("Objective OCL:"), lc);

        JPanel objRow = new JPanel(new BorderLayout(4, 0));
        objRow.add(objectiveField, BorderLayout.CENTER);
        objRow.add(validateBtn, BorderLayout.EAST);
        fc.gridy = row; fc.gridx = 1;
        panel.add(objRow, fc);
        row++;

        // Row 1: OCL validation status (spans col 1)
        GridBagConstraints sc = new GridBagConstraints();
        sc.gridx = 1; sc.gridy = row;
        sc.fill = GridBagConstraints.HORIZONTAL;
        sc.weightx = 1.0;
        sc.insets = new Insets(0, 0, 4, 4);
        panel.add(validateStatus, sc);
        row++;

        // Row 2: minimise checkbox + max-degree spinner
        minimiseBox.setToolTipText(
                "Checked: minimise objective (cost minimisation). "
                + "Unchecked: maximise objective (negated before QUBO encoding).");
        maxDegreeSpinner.setToolTipText(
                "Cap on pseudo-Boolean polynomial degree explored when degree-2 AutoQUBO sampling is not exact. "
                + "Terms above degree 2 are reduced to a QUBO via Rosenberg quadratization (ancillary variables). "
                + "Higher values sample O(n^k) points and may add many ancilla variables.");
        maxDegreeSpinner.setPreferredSize(new java.awt.Dimension(60, maxDegreeSpinner.getPreferredSize().height));

        JPanel minimiseRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        minimiseRow.add(minimiseBox);
        minimiseRow.add(new JLabel("Max poly. degree:"));
        minimiseRow.add(maxDegreeSpinner);

        lc.gridy = row; fc.gridy = row;
        lc.gridx = 0;  fc.gridx = 1;
        panel.add(new JLabel("Minimise:"), lc);
        panel.add(minimiseRow, fc);
        row++;

        // Row 3: association checkbox list
        lc.gridy = row; lc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Decision-var associations:"), lc);

        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        List<MAssociation> assocs = new ArrayList<>(model.associations());
        assocs.sort(Comparator.comparing(MAssociation::name));
        for (MAssociation assoc : assocs) {
            JCheckBox cb = new JCheckBox(assoc.name());
            cb.setToolTipText("Links of this association become binary decision variables.");
            assocCheckboxes.put(assoc.name(), cb);
            checkboxPanel.add(cb);
        }
        JScrollPane scroll = new JScrollPane(checkboxPanel);
        scroll.setPreferredSize(new java.awt.Dimension(0, 140));

        GridBagConstraints lpc = new GridBagConstraints();
        lpc.gridx = 1; lpc.gridy = row;
        lpc.fill = GridBagConstraints.BOTH;
        lpc.weightx = 1.0; lpc.weighty = 1.0;
        lpc.insets = new Insets(4, 0, 4, 4);
        panel.add(scroll, lpc);

        return panel;
    }

    private void validateOcl(JLabel statusLabel) {
        String expr = objectiveField.getText().trim();
        if (expr.isEmpty()) {
            statusLabel.setText("✗ Expression is empty");
            statusLabel.setForeground(Color.RED);
            return;
        }
        StringWriter errWriter = new StringWriter();
        Expression compiled = OCLCompiler.compileExpression(
                model, expr, "validate", new PrintWriter(errWriter), new VarBindings());
        if (compiled != null) {
            statusLabel.setText("✓ Valid");
            statusLabel.setForeground(new Color(0, 140, 0));
        } else {
            String msg = errWriter.toString().trim();
            statusLabel.setText("✗ " + (msg.isEmpty() ? "Compilation error" : msg));
            statusLabel.setForeground(Color.RED);
        }
    }

    private JPanel buildButtonPanel(File configFile) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));

        JButton save = new JButton("Save");
        save.addActionListener(e -> {
            save.setEnabled(false);
            try {
                doSave(configFile);
                PluginLog.info("QUBO config saved to " + configFile.getAbsolutePath());
                JOptionPane.showMessageDialog(QuboConfigView.this,
                        "Saved to:\n" + configFile.getAbsolutePath(),
                        "Edit QUBO Config", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                PluginLog.error("Failed to save config", ex);
                JOptionPane.showMessageDialog(QuboConfigView.this,
                        "Failed to save:\n" + ex.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                save.setEnabled(true);
            }
        });

        JButton close = new JButton("Close");
        close.addActionListener(e ->
                ((JInternalFrame) SwingUtilities.getAncestorOfClass(JInternalFrame.class, QuboConfigView.this)).dispose());

        panel.add(save);
        panel.add(close);
        return panel;
    }

    private void prefill(File configFile) {
        if (!configFile.exists()) return;
        try {
            String raw = new String(Files.readAllBytes(configFile.toPath()), StandardCharsets.UTF_8);
            QuboConfig cfg = QuboConfig.parse(raw);
            objectiveField.setText(cfg.objectiveExpr != null ? cfg.objectiveExpr : "");
            minimiseBox.setSelected(cfg.minimise);
            maxDegreeSpinner.setValue(cfg.maxDegree);
            for (String assocName : cfg.decisionVarAssocs) {
                JCheckBox cb = assocCheckboxes.get(assocName);
                if (cb != null) cb.setSelected(true);
            }
        } catch (IOException e) {
            PluginLog.warn("Could not pre-fill from config file", e);
        }
    }

    private void doSave(File configFile) throws IOException {
        List<String> assocNames = new ArrayList<>();
        for (Map.Entry<String, JCheckBox> e : assocCheckboxes.entrySet()) {
            if (e.getValue().isSelected()) assocNames.add(e.getKey());
        }
        String expr = objectiveField.getText().trim();
        boolean minimise = minimiseBox.isSelected();
        int maxDegree = (Integer) maxDegreeSpinner.getValue();

        SimpleJsonWriter w = new SimpleJsonWriter();
        w.objectOpen();
        w.linkArray("decision_var_associations", assocNames, true);
        w.key("decision_vars").arrayOpen().arrayClose(true);
        w.key("objective").objectOpen();
        w.keyValue("expression", expr, true);
        w.keyValue("minimise", minimise, true);
        w.keyValue("max_degree", maxDegree, false);
        w.objectClose(true);
        w.keyValue("objective_weight", 1, false);
        w.objectClose(false);

        Files.write(configFile.toPath(), w.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void detachModel() {}
}
