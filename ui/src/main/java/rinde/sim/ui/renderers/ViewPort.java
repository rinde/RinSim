/**
 * 
 */
package rinde.sim.ui.renderers;

import rinde.sim.core.graph.Point;

/**
 * Value object containing information about the region of the screen which is
 * used for rendering.
 * 
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class ViewPort {

  public final Point origin;
  public final ViewRect rect;
  public final double scale;

  public ViewPort(Point pOrigin, ViewRect pViewRect, double pZoom) {
    origin = pOrigin;
    rect = pViewRect;
    scale = pZoom;
  }

  public int toCoordX(double x) {
    return (int) (origin.x + ((x - rect.min.x) * scale));
  }

  public int toCoordY(double y) {
    return (int) (origin.y + ((y - rect.min.y) * scale));
  }

}
