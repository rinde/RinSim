package rinde.sim.ui.renderers;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;

import org.eclipse.swt.graphics.GC;

import rinde.sim.core.graph.Connection;
import rinde.sim.core.graph.ConnectionData;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.GraphRoadModel;
import rinde.sim.ui.SimulationViewer;

/**
 * 
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class RoadsRenderer implements ModelRenderer<GraphRoadModel> {
	protected GraphRoadModel grm;
	protected final int margin;

	public RoadsRenderer(int pMargin) {
		margin = pMargin;
	}

	public RoadsRenderer() {
		this(20);
	}

	@Override
	public void renderStatic(GC gc, ViewPort vp) {
		Graph<? extends ConnectionData> graph = grm.getGraph();
		for (Connection<? extends ConnectionData> e : graph.getConnections()) {
			int x1 = vp.toCoordX(e.from.x);
			int y1 = vp.toCoordY(e.from.y);

			int x2 = vp.toCoordX(e.to.x);
			int y2 = vp.toCoordY(e.to.y);
			gc.setForeground(vp.colorRegistry.get(SimulationViewer.COLOR_BLACK));
			gc.drawLine(x1, y1, x2, y2);
		}
	}

	@Override
	public void renderDynamic(GC gc, ViewPort vp) {}

	@Override
	public ViewRect getViewRect() {
		checkState(!grm.getGraph().isEmpty(), "graph may not be empty at this point");
		Collection<Point> nodes = grm.getGraph().getNodes();

		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (Point p : nodes) {
			minX = Math.min(minX, p.x);
			maxX = Math.max(maxX, p.x);
			minY = Math.min(minY, p.y);
			maxY = Math.max(maxY, p.y);
		}
		return new ViewRect(new Point(minX - margin, minY - margin), new Point(maxX + margin, maxY + margin));
	}

	@Override
	public void register(GraphRoadModel model) {
		grm = model;
	}

	@Override
	public Class<GraphRoadModel> getSupportedModelType() {
		return GraphRoadModel.class;
	}
}