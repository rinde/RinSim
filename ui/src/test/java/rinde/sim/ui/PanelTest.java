/**
 * 
 */
package rinde.sim.ui;

import org.apache.commons.math3.random.MersenneTwister;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.ui.renderers.PanelRenderer;
import rinde.sim.ui.renderers.PlaneRoadModelRenderer;
import rinde.sim.ui.renderers.RoadUserRenderer;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public final class PanelTest {

	private PanelTest() {}

	public static void main(String[] args) {

		final Simulator sim = new Simulator(new MersenneTwister(123), 1000);
		sim.register(new PlaneRoadModel(new Point(0, 0), new Point(10, 10), false, 10));
		sim.configure();

		View.startGui(sim, 1, new RoadUserRenderer(), new PlaneRoadModelRenderer(), new TestPanelRenderer("LEFT",
				SWT.LEFT, 200), new TestPanelRenderer("RIHGT BOEEE YEAH", SWT.RIGHT, 300), new TestPanelRenderer(
				"RIHGT BOEEE YEAH", SWT.TOP, 100), new TestPanelRenderer("TOP2", SWT.TOP, 100), new TestPanelRenderer(
				"LEFT2", SWT.LEFT, 100), new TestPanelRenderer("LEFT3", SWT.LEFT, 150));

	}

	static class TestPanelRenderer implements PanelRenderer {

		protected final String name;
		protected final int position;
		protected final int size;

		public TestPanelRenderer(String n, int pos, int s) {
			name = n;
			position = pos;
			size = s;
		}

		@Override
		public void setParent(Composite c) {
			// TODO Auto-generated method stub

		}

		@Override
		public int getPreferredPosition() {
			return position;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public int preferredSize() {
			return size;
		}

	}
}
