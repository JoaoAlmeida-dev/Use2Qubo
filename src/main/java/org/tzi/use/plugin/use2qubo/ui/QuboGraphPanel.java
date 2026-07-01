package org.tzi.use.plugin.use2qubo.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import org.tzi.use.plugin.use2qubo.qubo.QuboResult;
import org.tzi.use.plugin.use2qubo.util.QuboConstants;

public class QuboGraphPanel extends JPanel {

    private static final Color BLUE = new Color(100, 149, 237);
    private static final Color RED  = new Color(205, 92, 92);
    private static final Color GRAY = new Color(200, 200, 200);
    private static final double EPS = QuboConstants.EPS;

    private final QuboResult result;
    private final double maxLinear;
    private final double maxQuad;

    public QuboGraphPanel(QuboResult result) {
        this.result = result;

        double ml = 0.0;
        for (double v : result.linear.values()) ml = Math.max(ml, Math.abs(v));
        maxLinear = ml;

        double mq = 0.0;
        for (double v : result.quadratic.values()) mq = Math.max(mq, Math.abs(v));
        maxQuad = mq;

        setLayout(new BorderLayout());
        add(buildLegend(), BorderLayout.SOUTH);
        setBackground(Color.WHITE);

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateTooltip(e.getX(), e.getY());
            }
        });
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int n = result.nVars;
        if (n == 0 || getWidth() == 0 || getHeight() == 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        double[] cx = new double[n];
        double[] cy = new double[n];
        double[] nr = new double[n];
        computeLayout(cx, cy, nr);

        // Edges (drawn first, behind nodes)
        for (Map.Entry<String, Double> e : result.quadratic.entrySet()) {
            double v = e.getValue();
            if (Math.abs(v) < EPS) continue;
            String[] parts = e.getKey().split(",");
            int i = Integer.parseInt(parts[0]);
            int j = Integer.parseInt(parts[1]);
            if (i >= n || j >= n) continue;
            float strokeW = (float) (1.0 + (maxQuad > EPS
                    ? Math.abs(v) / maxQuad * QuboConstants.GRAPH_EDGE_WIDTH_SCALE : 1.0));
            Color base = v > 0 ? BLUE : RED;
            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 190));
            g2.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Double(cx[i], cy[i], cx[j], cy[j]));
        }

        // Nodes
        g2.setStroke(new BasicStroke(1.0f));
        for (int i = 0; i < n; i++) {
            double r = nr[i];
            double v = result.linear.getOrDefault(i, 0.0);
            Color fill = Math.abs(v) < EPS ? GRAY : (v > 0 ? BLUE : RED);
            g2.setColor(fill);
            g2.fill(new Ellipse2D.Double(cx[i] - r, cy[i] - r, r * 2, r * 2));
            g2.setColor(Color.BLACK);
            g2.draw(new Ellipse2D.Double(cx[i] - r, cy[i] - r, r * 2, r * 2));

            String label = ViewFormatUtil.abbrev(result.varLabels.get(i));
            FontMetrics fm = g2.getFontMetrics();
            int lw = fm.stringWidth(label);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(label, (float) (cx[i] - lw / 2.0), (float) (cy[i] + r + fm.getAscent() + 2));
        }

        // Constant label (top-left)
        g2.setColor(Color.DARK_GRAY);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
        g2.drawString(String.format("c = %.4f", result.constant), 10, 18);

        // Dense-graph warning overlay
        if (n > 50) {
            String warn = "Graph may be dense — use Terms tab for details";
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(warn);
            int bx = getWidth() / 2 - tw / 2 - 10;
            int by = getHeight() / 2 - 16;
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRoundRect(bx, by, tw + 20, 32, 8, 8);
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
            g2.drawString(warn, getWidth() / 2 - tw / 2, by + 20);
        }

        g2.dispose();
    }

    private void computeLayout(double[] cx, double[] cy, double[] nr) {
        int n = result.nVars;
        int legendH = 30;
        int labelH  = 28;
        int margin  = 24 + labelH;
        int w = getWidth();
        int h = getHeight() - legendH;
        double centreX = w / 2.0;
        double centreY = (h - margin) / 2.0 + margin / 2.0;
        double radius  = Math.max(60, Math.min(w, h - margin) / 2.0 - margin);

        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n - Math.PI / 2;
            cx[i] = centreX + radius * Math.cos(angle);
            cy[i] = centreY + radius * Math.sin(angle);
            double linV  = result.linear.getOrDefault(i, 0.0);
            double scale = maxLinear > EPS ? Math.abs(linV) / maxLinear : 0.0;
            nr[i] = QuboConstants.GRAPH_NODE_RADIUS_BASE + scale * QuboConstants.GRAPH_NODE_RADIUS_SCALE;
        }
    }

    private void updateTooltip(int mx, int my) {
        int n = result.nVars;
        if (n == 0 || getWidth() == 0) { setToolTipText(null); return; }

        double[] cx = new double[n];
        double[] cy = new double[n];
        double[] nr = new double[n];
        computeLayout(cx, cy, nr);

        // Check nodes first
        for (int i = 0; i < n; i++) {
            double dx = mx - cx[i];
            double dy = my - cy[i];
            if (Math.sqrt(dx * dx + dy * dy) <= nr[i] + QuboConstants.GRAPH_NODE_HIT_PAD) {
                double v = result.linear.getOrDefault(i, 0.0);
                setToolTipText("<html>" + result.varLabels.get(i)
                        + "<br>linear: " + String.format("%.6f", v) + "</html>");
                return;
            }
        }

        // Check edges
        String bestTip = null;
        double bestDist = QuboConstants.GRAPH_EDGE_HIT_TOLERANCE;
        for (Map.Entry<String, Double> e : result.quadratic.entrySet()) {
            if (Math.abs(e.getValue()) < EPS) continue;
            String[] parts = e.getKey().split(",");
            int i = Integer.parseInt(parts[0]);
            int j = Integer.parseInt(parts[1]);
            if (i >= n || j >= n) continue;
            double dist = pointToSegmentDist(mx, my, cx[i], cy[i], cx[j], cy[j]);
            if (dist < bestDist) {
                bestDist = dist;
                bestTip = "<html>" + result.varLabels.get(i) + " × " + result.varLabels.get(j)
                        + "<br>quadratic: " + String.format("%.6f", e.getValue()) + "</html>";
            }
        }
        setToolTipText(bestTip);
    }

    private static double pointToSegmentDist(double px, double py,
                                              double ax, double ay,
                                              double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double lenSq = dx * dx + dy * dy;
        double t = lenSq < 1e-12 ? 0.0
                : Math.max(0.0, Math.min(1.0, ((px - ax) * dx + (py - ay) * dy) / lenSq));
        double qx = ax + t * dx;
        double qy = ay + t * dy;
        double ex = px - qx;
        double ey = py - qy;
        return Math.sqrt(ex * ex + ey * ey);
    }

    private static JPanel buildLegend() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        p.setBorder(BorderFactory.createEtchedBorder());
        p.add(ViewFormatUtil.makeSwatch(RED,  "negative"));
        p.add(ViewFormatUtil.makeSwatch(BLUE, "positive"));
        JLabel note = new JLabel("node size = |linear|   edge width = |quadratic|");
        note.setFont(note.getFont().deriveFont(Font.PLAIN, 11f));
        note.setForeground(Color.GRAY);
        p.add(note);
        return p;
    }
}
