package com.github.rinde.rinsim.ui.renderers;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import com.github.rinde.rinsim.core.graph.Connection;
import com.github.rinde.rinsim.core.graph.ConnectionData;
import com.github.rinde.rinsim.core.graph.Graph;
import com.github.rinde.rinsim.core.graph.Point;
import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.google.common.base.Optional;

/**
 * 
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public final class GraphRoadModelRenderer implements ModelRenderer {

  private static final int NODE_RADIUS = 2;
  private static final Point RELATIVE_TEXT_POSITION = new Point(4, -14);

  private GraphRoadModel grm;
  private final int margin;
  private final boolean showNodes;
  private final boolean showNodeLabels;
  private final boolean drawDirectionArrows;

  public GraphRoadModelRenderer(int pMargin, boolean pShowNodes,
      boolean pShowNodeLabels, boolean pDrawDirectionArrows) {
    margin = pMargin;
    showNodes = pShowNodes;
    showNodeLabels = pShowNodeLabels;
    drawDirectionArrows = pDrawDirectionArrows;
  }

  /**
   * Creates a {@link GraphRoadModelRenderer} instance with a margin of
   * <code>20<code> and not displaying nodes or direction arrows.
   */
  public GraphRoadModelRenderer() {
    this(20, false, false, false);
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {
    final Graph<? extends ConnectionData> graph = grm.getGraph();

    if (showNodes || showNodeLabels) {
      for (final Point node : graph.getNodes()) {
        final int x1 = vp.toCoordX(node.x) - NODE_RADIUS;
        final int y1 = vp.toCoordY(node.y) - NODE_RADIUS;

        if (showNodes) {
          final int size = NODE_RADIUS * 2;
          gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
          gc.fillOval(x1, y1, size, size);
        }
        if (showNodeLabels) {
          gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
          gc.drawString(node.toString(), x1 + (int) RELATIVE_TEXT_POSITION.x,
              y1 + (int) RELATIVE_TEXT_POSITION.y, true);
        }
      }
    }

    for (final Connection<? extends ConnectionData> e : graph.getConnections()) {
      final int x1 = vp.toCoordX(e.from.x);
      final int y1 = vp.toCoordY(e.from.y);

      final int x2 = vp.toCoordX(e.to.x);
      final int y2 = vp.toCoordY(e.to.y);
      gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
      gc.drawLine(x1, y1, x2, y2);

      if (drawDirectionArrows) {
        final double dist = Point
            .distance(new Point(x1, y1), new Point(x2, y2));
        final double r = 14d / dist;
        final double r2 = 4d / dist;
        final Point unit = Point.divide(
            Point.diff(new Point(x1, y1), new Point(x2, y2)), dist);

        // get two points on the line
        final int x3 = (int) (r * x1 + (1 - r) * x2);
        final int y3 = (int) (r * y1 + (1 - r) * y2);
        final int x6 = (int) (r2 * x1 + (1 - r2) * x2);
        final int y6 = (int) (r2 * y1 + (1 - r2) * y2);

        // get two points perpendicular to the line next to point 3
        final int x4 = (int) (x3 + 5 * unit.y);
        final int y4 = (int) (y3 + 5 * unit.x);
        final int x5 = (int) (x3 - 5 * unit.y);
        final int y5 = (int) (y3 - 5 * unit.x);

        // draw the arrow
        gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
        gc.fillPolygon(new int[] { x4, y4, x5, y5, x6, y6 });
      }
    }
  }

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {}

  @Nullable
  @Override
  public ViewRect getViewRect() {
    checkState(!grm.getGraph().isEmpty(),
        "graph may not be empty at this point");
    final Collection<Point> nodes = grm.getGraph().getNodes();

    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;

    for (final Point p : nodes) {
      minX = Math.min(minX, p.x);
      maxX = Math.max(maxX, p.x);
      minY = Math.min(minY, p.y);
      maxY = Math.max(maxY, p.y);
    }
    return new ViewRect(new Point(minX - margin, minY - margin), new Point(maxX
        + margin, maxY + margin));
  }

  @Override
  public void registerModelProvider(ModelProvider mp) {
    grm = Optional.fromNullable(mp.getModel(GraphRoadModel.class)).get();
  }
}
