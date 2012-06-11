/**
 * 
 */
package rinde.sim.ui.renderers;

import org.eclipse.jface.resource.ColorRegistry;

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
	// TODO colorRegistry should be read-only
	public final ColorRegistry colorRegistry;

	public ViewPort(Point pOrigin, ViewRect pViewRect, double pZoom, ColorRegistry registry) {
		origin = pOrigin;
		rect = pViewRect;
		scale = pZoom;
		colorRegistry = registry;
	}

}
