/**
 * 
 */
package rinde.sim.ui.renderers;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.road.RoadUser;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class RandomObjectRenderer implements Renderer {

	private final RoadModel model;
	private Color defaultColor;

	public RandomObjectRenderer(RoadModel model) {
		this.model = model;
	}

	@Override
	public void renderDynamic(GC gc, ViewPort viewPort, long time) {
		final int radius = 4;
		final int outerRadius = 10;
		if (defaultColor == null) {
			defaultColor = new Color(gc.getDevice(), 255, 0, 0);
		}

		gc.setBackground(defaultColor);

		final Map<RoadUser, Point> objects = model.getObjectsAndPositions();
		synchronized (objects) {
			for (final Entry<RoadUser, Point> entry : objects.entrySet()) {
				final Point p = entry.getValue();
				// if (colorMap != null) {
				// if (colorMap.containsKey(entry.getKey().getClass())) {
				// gc.setBackground(new Color(gc.getDevice(),
				// colorMap.get(entry.getKey().getClass())));
				// } else {
				// gc.setBackground(new Color(gc.getDevice(), 255, 0, 0));
				// }
				// }
				// if (useEncirclement) {
				// gc.setForeground(gc.getBackground());
				// gc.drawOval((int) (xOrigin + (p.x - minX) * m) - outerRadius,
				// (int) (yOrigin + (p.y - minY) * m) - outerRadius, 2 *
				// outerRadius, 2 * outerRadius);
				// }
				gc.setBackground(defaultColor);

				final int x = (int) (viewPort.origin.x + (p.x - viewPort.rect.min.x) * viewPort.scale) - radius;
				final int y = (int) (viewPort.origin.y + (p.y - viewPort.rect.min.y) * viewPort.scale) - radius;

				gc.fillOval(x, y, 2 * radius, 2 * radius);
				gc.drawText(entry.getKey() + "", x + 5, y - 15);
			}
		}
	}

	@Override
	public void renderStatic(GC gc, ViewPort vp) {}

	@Override
	public ViewRect getViewRect() {
		return null;
	}
}
