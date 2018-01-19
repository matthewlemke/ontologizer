package ontologizer.gui.swt.support;

import java.awt.geom.PathIterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import att.grappa.Edge;
import att.grappa.Element;
import att.grappa.Graph;
import att.grappa.GraphEnumeration;
import att.grappa.GrappaConstants;
import att.grappa.GrappaLine;
import att.grappa.GrappaNexus;
import att.grappa.GrappaPoint;
import att.grappa.GrappaStyle;
import att.grappa.Node;
import att.grappa.Subgraph;

/**
 * This class is responsible for drawing a Graph.
 *
 * @author Sebastian Bauer
 */
public class GraphPaint
{
    private Display display;

    private Graph g;

    private Node selectedNode;

    private HashMap<Node, Path> node2Path;

    public GraphPaint(Display display, Graph g)
    {
        this.display = display;
        this.g = g;

        this.node2Path = new HashMap<>();
        prepareElement(g);
    }

    /**
     * Draws the graph on the gc.
     *
     * @param gc
     */
    public void drawGraph(GC gc)
    {
        paintElement(this.g, gc);
    }

    /**
     * Changes the state of the graph paint such that the given Node is selected.
     *
     * @param selectedNode
     */
    public void setSelectedNode(Node selectedNode)
    {
        this.selectedNode = selectedNode;
    }

    /**
     * Returns the currently selected node.
     *
     * @return
     */
    public Node getSelectedNode()
    {
        return this.selectedNode;
    }

    /**
     * Prepare the given element.
     *
     * @param e
     */
    private void prepareElement(Element e)
    {
        switch (e.getType()) {
            case GrappaConstants.NODE: {
                Node node = (Node) e;
                GrappaPoint center = node.getCenterPoint();

                int x = (int) center.x;
                int y = (int) center.y;
                double w = (Double) node.getAttributeValue(GrappaConstants.WIDTH_ATTR);
                double h = (Double) node.getAttributeValue(GrappaConstants.HEIGHT_ATTR);
                int shape = (Integer) node.getAttributeValue(GrappaConstants.SHAPE_ATTR);

                w *= 72;
                h *= 72;

                Path nodePath = new Path(this.display);
                switch (shape) {
                    case GrappaConstants.PLAINTEXT_SHAPE:
                        break;

                    case GrappaConstants.OVAL_SHAPE:
                        nodePath.addArc((float) (x - w / 2), (float) (y - h / 2), (float) w, (float) h, 0f, 360f);
                        break;

                    default:
                        nodePath.addPath(pathIterator2Path(node.getGrappaNexus().getPathIterator()));
                        break;
                }
                this.node2Path.put(node, nodePath);
            }
                break;

            case GrappaConstants.SUBGRAPH: {
                GraphEnumeration en = ((Subgraph) e).elements();

                while (en.hasMoreElements()) {
                    Element ne = en.nextGraphElement();
                    if (e != ne) {
                        prepareElement(ne);
                    }
                }
            }
                break;
        }
    }

    private void paintElement(Element e, GC gc)
    {
        switch (e.getType()) {
            case GrappaConstants.NODE:
                paintNode((Node) e, gc);
                break;

            case GrappaConstants.EDGE:
                paintEdge((Edge) e, gc);
                break;

            case GrappaConstants.SUBGRAPH: {
                GraphEnumeration en = ((Subgraph) e).elements();

                while (en.hasMoreElements()) {
                    Element ne = en.nextGraphElement();
                    if (e != ne) {
                        paintElement(ne, gc);
                    }
                }
            }
                break;
        }
    }

    /**
     * Paint the given edge into gc.
     *
     * @param e
     * @param gc
     */
    private void paintEdge(Edge e, GC gc)
    {
        GrappaLine line = (GrappaLine) e.getAttributeValue(GrappaConstants.POS_ATTR);
        java.awt.Color col = (java.awt.Color) e.getAttributeValue(GrappaConstants.COLOR_ATTR);
        Color swtCol = null;
        Color oldCol = null;

        if (col != null) {
            swtCol = new Color(this.display, col.getRed(), col.getGreen(), col.getBlue());
            oldCol = gc.getForeground();
            gc.setForeground(swtCol);
        }

        PathIterator iter = line.getPathIterator();
        Path path = pathIterator2Path(iter);
        gc.drawPath(path);
        path.dispose();

        if (swtCol != null) {
            gc.setForeground(oldCol);
            swtCol.dispose();
        }
    }

    /**
     * Paint the given node into gc.
     *
     * @param node
     * @param gc
     */
    private void paintNode(Node node, GC gc)
    {
        GrappaPoint center = node.getCenterPoint();

        int x = (int) center.x;
        int y = (int) center.y;
        double h = (Double) node.getAttributeValue(GrappaConstants.HEIGHT_ATTR);
        String label = (String) node.getAttributeValue(GrappaConstants.LABEL_ATTR);
        GrappaStyle style = (GrappaStyle) node.getAttributeValue(GrappaConstants.STYLE_ATTR);
        @SuppressWarnings("unchecked")
        List<java.awt.Color> fillColorAWT =
            (List<java.awt.Color>) node.getAttributeValue(GrappaConstants.FILLCOLOR_ATTR);
        int shape = (Integer) node.getAttributeValue(GrappaConstants.SHAPE_ATTR);

        h *= 72;

        Path nodePath = this.node2Path.get(node);
        assert (nodePath != null);

        if (shape != GrappaConstants.PLAINTEXT_SHAPE) {
            if (fillColorAWT != null) {
                boolean gradient_fill = fillColorAWT.size() > 1;

                Color oldFillColor = gc.getBackground();

                Color topColor = new Color(this.display, fillColorAWT.get(0).getRed(), fillColorAWT.get(0).getGreen(),
                    fillColorAWT.get(0).getBlue());
                Color bottomColor = null;

                gc.setBackground(topColor);

                Pattern oldPat = gc.getBackgroundPattern();
                Pattern pat = null;
                if (gradient_fill) {
                    bottomColor = new Color(this.display, fillColorAWT.get(1).getRed(), fillColorAWT.get(1).getGreen(),
                        fillColorAWT.get(1).getBlue());
                    pat = new Pattern(this.display, 0, y - (float) h / 2, 0, y + (float) h / 2, topColor, bottomColor);
                    gc.setBackgroundPattern(pat);
                }

                gc.fillPath(nodePath);
                gc.setBackground(oldFillColor);
                if (gradient_fill) {
                    gc.setBackgroundPattern(oldPat);
                    pat.dispose();
                    bottomColor.dispose();
                }
                topColor.dispose();
            }

            int oldLineWidth = gc.getLineWidth();
            gc.setLineWidth((int) (style.getLineWidth()));

            if (node == this.selectedNode) {
                gc.setForeground(this.display.getSystemColor(SWT.COLOR_RED));
                gc.setLineWidth(3);
            } else {
                gc.setForeground(this.display.getSystemColor(SWT.COLOR_BLACK));
            }
            gc.drawPath(nodePath);
            gc.setLineWidth(oldLineWidth);

            if (node == this.selectedNode) {
                gc.setForeground(this.display.getSystemColor(SWT.COLOR_BLACK));
            }
        }

        if (label != null) {
            String[] labelArray = label.split("\\\\n");
            Point[] pointArray = new Point[labelArray.length];
            int height = 0;

            for (int i = 0; i < labelArray.length; i++) {
                pointArray[i] = gc.textExtent(labelArray[i]);
                height += pointArray[i].y;
            }

            int cy = y - height / 2;
            for (int i = 0; i < labelArray.length; i++) {
                gc.drawString(labelArray[i], x - pointArray[i].x / 2, cy, true);
                cy += pointArray[i].y;
            }
        }
    }

    /**
     * Converts a java 2d path iterator to a SWT path.
     *
     * @param iter
     * @return
     */
    private Path pathIterator2Path(PathIterator iter)
    {
        float[] coords = new float[6];

        Path path = new Path(this.display);

        while (!iter.isDone()) {
            int type = iter.currentSegment(coords);

            switch (type) {
                case PathIterator.SEG_MOVETO:
                    path.moveTo(coords[0], coords[1]);
                    break;

                case PathIterator.SEG_LINETO:
                    path.lineTo(coords[0], coords[1]);
                    break;

                case PathIterator.SEG_CLOSE:
                    path.close();
                    break;

                case PathIterator.SEG_QUADTO:
                    path.quadTo(coords[0], coords[1], coords[2], coords[3]);
                    break;

                case PathIterator.SEG_CUBICTO:
                    path.cubicTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                    break;
            }

            iter.next();
        }
        return path;
    }

    /**
     * Dispose all resources associated to the GraphPaint.
     */
    public void dispose()
    {
        for (Path p : this.node2Path.values()) {
            p.dispose();
        }
        this.node2Path = null;
    }

    /**
     * Returns the node below the given coordinate.
     *
     * @param x
     * @param y
     * @return
     */
    public Node findNodeByCoord(float x, float y)
    {
        float[] points = new float[2];
        float[] bounds = new float[4];

        points[0] = x;
        points[1] = y;

        for (Entry<Node, Path> entry : this.node2Path.entrySet()) {
            Path p = entry.getValue();
            p.getBounds(bounds);
            if (points[0] > bounds[0] && points[0] < bounds[0] + bounds[2] &&
                points[1] > bounds[1] && points[1] < bounds[1] + bounds[3]) {
                return entry.getKey();
            }
        }
        return null;
    }

    private Edge findEdgeByCoord(Element e, float x, float y)
    {
        switch (e.getType()) {
            case GrappaConstants.EDGE: {
                Edge edge = (Edge) e;
                GrappaNexus gn = edge.getGrappaNexus();
                if (gn.contains(x, y)) {
                    return edge;
                }
            }
                break;

            case GrappaConstants.SUBGRAPH: {
                GraphEnumeration en = ((Subgraph) e).elements();

                while (en.hasMoreElements()) {
                    Element ne = en.nextGraphElement();
                    if (e != ne) {
                        Edge fe = findEdgeByCoord(ne, x, y);
                        if (fe != null) {
                            return fe;
                        }
                    }
                }
            }
                break;

        }
        return null;
    }

    /**
     * Returns the edge below the given coordinate.
     *
     * @param x
     * @param y
     * @return
     */
    public Edge findEdgeByCoord(float x, float y)
    {
        return findEdgeByCoord(this.g, x, y);
    }

}
