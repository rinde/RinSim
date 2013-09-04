package rinde.sim.ui.renderers;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import rinde.sim.core.graph.Connection;
import rinde.sim.core.graph.ConnectionData;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.core.model.road.GraphRoadModel;

/**
 * 
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class GraphRoadModelRenderer implements ModelRenderer {
  protected GraphRoadModel grm;
  protected final int margin;

  public GraphRoadModelRenderer(int pMargin) {
    margin = pMargin;
  }

  public GraphRoadModelRenderer() {
    this(20);
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {
    final Graph<? extends ConnectionData> graph = grm.getGraph();
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
    grm = mp.getModel(GraphRoadModel.class);
  }

}
