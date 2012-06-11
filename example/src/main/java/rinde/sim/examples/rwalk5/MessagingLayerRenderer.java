package rinde.sim.examples.rwalk5;

import java.util.Set;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.ui.renderers.Renderer;
import rinde.sim.ui.renderers.UiSchema;
import rinde.sim.ui.renderers.ViewPort;
import rinde.sim.ui.renderers.ViewRect;

public class MessagingLayerRenderer implements Renderer {

	protected RoadModel rs;
	protected boolean useEncirclement;
	private final UiSchema uiSchema;

	public MessagingLayerRenderer(RoadModel rs, UiSchema uiSchema) {
		this.rs = rs;
		this.uiSchema = uiSchema;
	}

	@Override
	public void renderDynamic(GC gc, ViewPort vp) {
		final int size = 4;
		uiSchema.initialize();

		Set<RandomWalkAgent> objects = rs.getObjectsOfType(RandomWalkAgent.class);

		synchronized (objects) {
			for (RandomWalkAgent a : objects) {
				Point p = a.getPosition();
				if (p == null) {
					continue;
				}
				final int x = (int) (vp.origin.x + (p.x - vp.rect.min.x) * vp.scale);
				final int y = (int) (vp.origin.y + (p.y - vp.rect.min.y) * vp.scale);

				final int radius = (int) (a.getRadius() * vp.scale);

				Color c = null;
				if (a.getReliability() < 0.15) {
					c = uiSchema.getColor(RandomWalkAgent.C_BLACK);
				} else if (a.getReliability() >= 0.15 && a.getReliability() < 0.3) {
					c = uiSchema.getColor(RandomWalkAgent.C_YELLOW);
				} else {
					c = uiSchema.getColor(RandomWalkAgent.C_GREEN);
				}

				gc.setForeground(c);
				gc.setBackground(c);

				gc.fillOval(x - size, y - size, size * 2, size * 2);

				gc.drawOval(x - radius, y - radius, radius * 2, radius * 2);
				gc.drawText("r:" + a.getNoReceived(), x, y, true);

				Set<RandomWalkAgent> communicatedWith = a.getCommunicatedWith();
				for (RandomWalkAgent cw : communicatedWith) {
					p = cw.getPosition();
					if (p == null) {
						continue;
					}
					final int xCW = (int) (vp.origin.x + (p.x - vp.rect.min.x) * vp.scale);
					final int yCW = (int) (vp.origin.y + (p.y - vp.rect.min.y) * vp.scale);
					gc.drawLine(x, y, xCW, yCW);
				}
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
