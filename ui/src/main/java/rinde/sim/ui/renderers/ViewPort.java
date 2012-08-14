/**
 * 
 */
package rinde.sim.ui.renderers;

import static java.util.Collections.unmodifiableMap;

import java.util.Map;

import org.eclipse.swt.graphics.Color;

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
	public final Map<String, Color> colorRegistry;

	public ViewPort(Point pOrigin, ViewRect pViewRect, double pZoom, Map<String, Color> registry) {
		origin = pOrigin;
		rect = pViewRect;
		scale = pZoom;
		colorRegistry = unmodifiableMap(registry);
	}

	public int toCoordX(double x) {
		return (int) (origin.x + ((x - rect.min.x) * scale));
	}

	public int toCoordY(double y) {
		return (int) (origin.y + ((y - rect.min.y) * scale));
	}

}
