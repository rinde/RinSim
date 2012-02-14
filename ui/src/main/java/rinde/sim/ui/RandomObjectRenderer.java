/**
 * 
 */
package rinde.sim.ui;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;

import rinde.sim.core.RoadModel;
import rinde.sim.core.RoadUser;
import rinde.sim.core.graph.Point;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class RandomObjectRenderer implements Renderer {

	private final RoadModel model;

	public RandomObjectRenderer(RoadModel model) {
		this.model = model;
	}

	@Override
	public void render(GC gc, double xOrigin, double yOrigin, double minX, double minY, double m) {
		final int radius = 4;
		final int outerRadius = 10;
		gc.setBackground(new Color(gc.getDevice(), 255, 0, 0));

		Map<RoadUser, Point> objects = model.getObjectsAndPositions();
		synchronized (objects) {
			for (Entry<RoadUser, Point> entry : objects.entrySet()) {
				Point p = entry.getValue();
				//				if (colorMap != null) {
				//					if (colorMap.containsKey(entry.getKey().getClass())) {
				//						gc.setBackground(new Color(gc.getDevice(), colorMap.get(entry.getKey().getClass())));
				//					} else {
				//						gc.setBackground(new Color(gc.getDevice(), 255, 0, 0));
				//					}
				//				}
				//				if (useEncirclement) {
				//					gc.setForeground(gc.getBackground());
				//					gc.drawOval((int) (xOrigin + (p.x - minX) * m) - outerRadius, (int) (yOrigin + (p.y - minY) * m) - outerRadius, 2 * outerRadius, 2 * outerRadius);
				//				}
				gc.setBackground(new Color(gc.getDevice(), 255, 0, 0));

				int x = (int) (xOrigin + (p.x - minX) * m) - radius;
				int y = (int) (yOrigin + (p.y - minY) * m) - radius;

				gc.fillOval(x, y, 2 * radius, 2 * radius);
				gc.drawText(entry.getKey() + "", x + 5, y - 15);
			}
		}
	}
}
