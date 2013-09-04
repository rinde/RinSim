/**
 * 
 */
package rinde.sim.ui.renderers;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.core.model.road.RoadModel;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public final class PlaneRoadModelRenderer implements ModelRenderer {

  private RoadModel rm;
  private final double margin;

  private double xMargin;
  private double yMargin;

  private List<Point> bounds;

  public PlaneRoadModelRenderer() {
    this(0.02);
  }

  public PlaneRoadModelRenderer(double pMargin) {
    margin = pMargin;
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {
    final int xMin = vp.toCoordX(bounds.get(0).x);
    final int yMin = vp.toCoordY(bounds.get(0).y);
    final int xMax = vp.toCoordX(bounds.get(1).x);
    final int yMax = vp.toCoordY(bounds.get(1).y);

    final int outerXmin = vp.toCoordX(vp.rect.min.x);
    final int outerYmin = vp.toCoordY(vp.rect.min.y);
    final int outerXmax = vp.toCoordX(vp.rect.max.x);
    final int outerYmax = vp.toCoordY(vp.rect.max.y);

    gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    gc.fillRectangle(outerXmin, outerYmin, outerXmax, outerYmax);

    gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
    gc.fillRectangle(xMin, yMin, xMax - xMin, yMax - yMin);
    gc.drawRectangle(xMin, yMin, xMax - xMin, yMax - yMin);
  }

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {}

  @Override
  public ViewRect getViewRect() {
    return new ViewRect(new Point(bounds.get(0).x - xMargin, bounds.get(0).y
        - yMargin), new Point(bounds.get(1).x + xMargin, bounds.get(1).y
        + yMargin));
  }

  @Override
  public void registerModelProvider(ModelProvider mp) {
    rm = mp.getModel(RoadModel.class);
    bounds = rm.getBounds();
    final double width = bounds.get(1).x - bounds.get(0).x;
    final double height = bounds.get(1).y - bounds.get(0).y;
    xMargin = width * margin;
    yMargin = height * margin;
  }
}
