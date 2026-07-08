package org.tzi.use.plugin.use2qubo.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.tzi.use.plugin.use2qubo.qubo.result.QuboResult;

/** Renders the QUBO polynomial as a readable algebraic expression, with copy-to-clipboard. */
public class ExpressionPanel extends JPanel {

    public ExpressionPanel(QuboResult result) {
        super(new BorderLayout(0, 4));
        String text = buildExpressionText(result);

        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JButton copy = new JButton("Copy");
        copy.addActionListener(e ->
                Toolkit.getDefaultToolkit().getSystemClipboard()
                       .setContents(new StringSelection(text), null));

        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.setBorder(BorderFactory.createEmptyBorder(4, 4, 2, 4));
        header.add(copy, BorderLayout.WEST);
        header.add(new JLabel("QUBO Expression"), BorderLayout.CENTER);

        setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));
        add(header, BorderLayout.NORTH);
        add(new JScrollPane(area), BorderLayout.CENTER);
    }

    private static String buildExpressionText(QuboResult result) {
        StringBuilder sb = new StringBuilder();
        String pad = "         "; // aligns continuation lines under the value after "f(x) = "

        sb.append(String.format("f(x) = %.6f%n", result.constant));

        for (int i = 0; i < result.nVars; i++) {
            double v = result.linear.getOrDefault(i, 0.0);
            if (Math.abs(v) < 1e-9) continue;
            String sign = v > 0 ? "+" : "-";
            sb.append(String.format("%s%s %.6f·x%s   [%s]%n",
                    pad, sign, Math.abs(v), subscript(i), result.varLabels.get(i)));
        }

        List<Map.Entry<String, Double>> quadEntries = new ArrayList<>(result.quadratic.entrySet());
        quadEntries.sort((a, b) -> {
            String[] pa = a.getKey().split(","), pb = b.getKey().split(",");
            int ai = Integer.parseInt(pa[0]), aj = Integer.parseInt(pa[1]);
            int bi = Integer.parseInt(pb[0]), bj = Integer.parseInt(pb[1]);
            return ai != bi ? ai - bi : aj - bj;
        });
        for (Map.Entry<String, Double> e : quadEntries) {
            double v = e.getValue();
            if (Math.abs(v) < 1e-9) continue;
            String[] parts = e.getKey().split(",");
            int i = Integer.parseInt(parts[0]);
            int j = Integer.parseInt(parts[1]);
            String sign = v > 0 ? "+" : "-";
            sb.append(String.format("%s%s %.6f·x%sx%s   [%s × %s]%n",
                    pad, sign, Math.abs(v),
                    subscript(i), subscript(j),
                    result.varLabels.get(i), result.varLabels.get(j)));
        }

        return sb.toString().trim();
    }

    private static String subscript(int n) {
        StringBuilder sb = new StringBuilder();
        for (char c : String.valueOf(n).toCharArray()) {
            sb.append((char) ('₀' + (c - '0')));
        }
        return sb.toString();
    }
}
