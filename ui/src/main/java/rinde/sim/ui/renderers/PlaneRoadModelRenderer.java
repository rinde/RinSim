/**
 * 
 */
package rinde.sim.ui.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.core.model.road.PlaneRoadModel;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class PlaneRoadModelRenderer implements ModelRenderer {

	protected PlaneRoadModel rm;
	protected final double margin;

	protected double xMargin;
	protected double yMargin;

	public PlaneRoadModelRenderer() {
		this(0.02);
	}

	public PlaneRoadModelRenderer(double pMargin) {
		margin = pMargin;
	}

	@Override
	public void renderStatic(GC gc, ViewPort vp) {
		final int xMin = vp.toCoordX(rm.min.x);
		final int yMin = vp.toCoordY(rm.min.y);
		final int xMax = vp.toCoordX(rm.max.x);
		final int yMax = vp.toCoordY(rm.max.y);

		final int outerXmin = vp.toCoordX(vp.rect.min.x);
		final int outerYmin = vp.toCoordY(vp.rect.min.y);
		final int outerXmax = vp.toCoordX(vp.rect.max.x);
		final int outerYmax = vp.toCoordY(vp.rect.max.y);

		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		gc.fillRectangle(outerXmin, outerYmin, outerXmax, outerYmax);

		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
		gc.fillRectangle(xMin, yMin, xMax - xMin, yMax - yMin);
		gc.drawRectangle(xMin, yMin, xMax - xMin, yMax - yMin);
	}

	@Override
	public void renderDynamic(GC gc, ViewPort vp, long time) {}

	@Override
	public ViewRect getViewRect() {
		return new ViewRect(new Point(rm.min.x - xMargin, rm.min.y - yMargin), new Point(rm.max.x + xMargin, rm.max.y
				+ yMargin));
	}

	@Override
	public void registerModelProvider(ModelProvider mp) {
		rm = mp.getModel(PlaneRoadModel.class);
		final double width = rm.max.x - rm.min.x;
		final double height = rm.max.y - rm.min.y;
		xMargin = width * margin;
		yMargin = height * margin;
	}
}
