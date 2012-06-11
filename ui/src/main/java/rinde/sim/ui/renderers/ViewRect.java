/**
 * 
 */
package rinde.sim.ui.renderers;

import rinde.sim.core.graph.Point;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class ViewRect {

	public final Point min;
	public final Point max;
	public final double width;
	public final double height;

	public ViewRect(Point pMin, Point pMax) {
		min = pMin;
		max = pMax;
		width = max.x - min.x;
		height = max.y - min.y;
	}

}
