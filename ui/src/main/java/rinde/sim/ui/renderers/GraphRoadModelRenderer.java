package rinde.sim.ui.renderers;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import rinde.sim.core.graph.Connection;
import rinde.sim.core.graph.ConnectionData;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.core.model.road.GraphRoadModel;

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

  public GraphRoadModelRenderer(int pMargin, boolean pShowNodes) {
    margin = pMargin;
    showNodes = pShowNodes;
  }

  public GraphRoadModelRenderer() {
    this(20, false);
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {
    final Graph<? extends ConnectionData> graph = grm.getGraph();

    if (showNodes) {
      for (final Point node : graph.getNodes()) {
        final int x1 = vp.toCoordX(node.x) - NODE_RADIUS;
        final int y1 = vp.toCoordY(node.y) - NODE_RADIUS;
        final int size = NODE_RADIUS * 2;
        gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
        gc.fillOval(x1, y1, size, size);
        gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
        gc.drawString(node.toString(), x1 + (int) RELATIVE_TEXT_POSITION.x, y1
            + (int) RELATIVE_TEXT_POSITION.y, true);
      }
    }
    for (final Connection<? extends ConnectionData> e : graph.getConnections()) {
      final int x1 = vp.toCoordX(e.from.x);
      final int y1 = vp.toCoordY(e.from.y);

      final int x2 = vp.toCoordX(e.to.x);
      final int y2 = vp.toCoordY(e.to.y);
      gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
      gc.drawLine(x1, y1, x2, y2);
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
