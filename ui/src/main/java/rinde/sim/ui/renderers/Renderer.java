/**
 * 
 */
package rinde.sim.ui.renderers;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public interface Renderer {

	/**
	 * @param gc
	 * @param origin
	 * @param minX
	 * @param minY
	 * @param scale
	 */
	void render(GC gc, double xOrigin, double yOrigin, double minX, double minY, double scale);

}
