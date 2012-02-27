/**
 * 
 */
package rinde.sim.ui.renderers;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.RoadUser;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be> changes in handling colors
 * 
 */
public class ObjectRenderer implements Renderer {
	
	protected RoadModel rs;
	protected boolean useEncirclement;
	private UiSchema uiSchema;

	public ObjectRenderer(RoadModel rs) {
		this(rs, new UiSchema(false), false);
		
	}

	public ObjectRenderer(RoadModel rs, UiSchema schema, boolean useEncirclement) {
		if(schema == null) schema = new UiSchema(false);
		this.rs = rs;
		this.useEncirclement = useEncirclement;
		this.uiSchema = schema;
	}

	@Override
	public void render(GC gc, double xOrigin, double yOrigin, double minX, double minY, double m) {
		final int radius = 4;
		final int outerRadius = 10;
		uiSchema.initialize();
		gc.setBackground(uiSchema.getDefaultColor());

		Map<RoadUser, Point> objects = rs.getObjectsAndPositions();
		synchronized (objects) {
			for (Entry<RoadUser, Point> entry : objects.entrySet()) {
				Point p = entry.getValue();
				Class<?> type = entry.getKey().getClass();
				final Image image = uiSchema.getImage(type);
				final int x = (int) (xOrigin + (p.x - minX) * m) - radius;
				final int y = (int) (yOrigin + (p.y - minY) * m) - radius;
				if(image != null) {
					int offsetX = x - image.getBounds().width / 2;
					int offsetY = y - image.getBounds().height / 2;
					gc.drawImage(image, offsetX, offsetY);
				} else {
					final Color color = uiSchema.getColor(type);
					if(color == null) continue;
					gc.setBackground(color);
					if (useEncirclement) {
						gc.setForeground(gc.getBackground());
						gc.drawOval((int) (xOrigin + (p.x - minX) * m) - outerRadius, (int) (yOrigin + (p.y - minY) * m) - outerRadius, 2 * outerRadius, 2 * outerRadius);
					}
					gc.fillOval((int) (xOrigin + (p.x - minX) * m) - radius, (int) (yOrigin + (p.y - minY) * m) - radius, 2 * radius, 2 * radius);
				}
				
			}
		}
	}

}
