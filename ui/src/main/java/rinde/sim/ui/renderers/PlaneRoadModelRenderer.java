/**
 * 
 */
package rinde.sim.ui.renderers;

import org.eclipse.swt.graphics.GC;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.PlaneRoadModel;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class PlaneRoadModelRenderer implements ModelRenderer {

	protected PlaneRoadModel rm;
	protected final int margin;

	public PlaneRoadModelRenderer() {
		this(0);
	}

	public PlaneRoadModelRenderer(int pMargin) {
		margin = pMargin;
	}

	@Override
	public void renderStatic(GC gc, ViewPort vp) {
		final int xMin = vp.toCoordX(rm.min.x) - margin;
		final int yMin = vp.toCoordY(rm.min.y) - margin;
		final int xMax = vp.toCoordX(rm.max.x) + margin;
		final int yMax = vp.toCoordY(rm.max.y) + margin;
		gc.drawRectangle(xMin, yMin, xMax - xMin, yMax - yMin);
	}

	@Override
	public void renderDynamic(GC gc, ViewPort vp, long time) {}

	@Override
	public ViewRect getViewRect() {

		return new ViewRect(new Point(rm.min.x - margin, rm.min.y - margin), new Point(rm.max.x + margin, rm.max.y
				+ margin));
	}

	@Override
	public void registerModelProvider(ModelProvider mp) {
		rm = mp.getModel(PlaneRoadModel.class);
		System.out.println("RM: " + rm);
	}

}
