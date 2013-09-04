/**
 * 
 */
package rinde.sim.ui.renderers;

import javax.annotation.Nullable;

import org.eclipse.swt.graphics.GC;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public interface CanvasRenderer extends Renderer {

  // FIXME documentation!

  /**
   * @param gc
   * @param origin
   * @param minX
   * @param minY
   * @param scale
   */

  void renderStatic(GC gc, ViewPort vp);

  void renderDynamic(GC gc, ViewPort vp, long time);

  @Nullable
  ViewRect getViewRect();

}
