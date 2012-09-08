/**
 * 
 */
package rinde.sim.examples.rwalk3;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultiAttributeData;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.core.model.communication.CommunicationModel;
import rinde.sim.core.model.road.GraphRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.road.RoadModels;
import rinde.sim.core.model.road.RoadUser;
import rinde.sim.examples.common.Package;
import rinde.sim.serializers.DotGraphSerializer;
import rinde.sim.serializers.SelfCycleFilter;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.GraphRoadModelRenderer;
import rinde.sim.ui.renderers.ModelRenderer;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.UiSchema;
import rinde.sim.ui.renderers.ViewPort;
import rinde.sim.ui.renderers.ViewRect;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class RandomWalkExample {

	public static void main(String[] args) throws Exception {

		final String MAP_DIR = "../core/files/maps/";
		// create a new simulator, load map of Leuven
		final RandomGenerator rand = new MersenneTwister(123);
		final Simulator simulator = new Simulator(rand, 1000);
		final Graph<MultiAttributeData> graph = DotGraphSerializer
				.getMultiAttributeGraphSerializer(new SelfCycleFilter())
				// .read("/Users/rindevanlon/Downloads/dot-files/brussels.dot");
				.read(MAP_DIR + "leuven-simple.dot");
		// roadModel.addGraph(DotGraphSerializer.getLengthGraphSerializer(new
		// SelfCycleFilter()).read("files/brussels.dot"));
		final RoadModel roadModel = new GraphRoadModel(graph);

		// XXX [bm] to be decided either Communication model have RG as a
		// constructor parameter or implements Simulator user interface
		final CommunicationModel communicationModel = new CommunicationModel(rand);
		simulator.register(roadModel);
		simulator.register(communicationModel);
		simulator.configure();

		final RandomGenerator r = new MersenneTwister(1317);
		for (int i = 0; i < 15; i++) {
			final RandomWalkAgent agent = new RandomWalkAgent(r.nextDouble() * 100);
			simulator.register(agent);

			for (int j = 0; j < 2; j++) {
				simulator.register(new Package("name", roadModel.getRandomPosition(r)));
			}
		}

		// // GUI stuff: agents are red, packages are blue or have ico
		// represenation
		final UiSchema schema = new UiSchema();
		// schema.add(RandomWalkAgent.class, new RGB(255, 0, 0));
		schema.add(RandomWalkAgent.class, "/graphics/perspective/semi-truck-32.png");
		schema.add(Package.class, "/graphics/perspective/deliverypackage.png");
		// schema.add(Package.class, new RGB(0x0, 0x0, 0xFF));

		// View.setTestingMode(true);
		View.startGui(simulator, 5, new GraphRoadModelRenderer(20), new FancyRenderer(), new RoadUserRenderer(schema,
				false));
	}

	static class FancyRenderer implements ModelRenderer {

		protected RoadModel rm;

		@Override
		public void renderStatic(GC gc, ViewPort vp) {
			// TODO Auto-generated method stub

		}

		@Override
		public void renderDynamic(GC gc, ViewPort vp, long time) {
			// TODO Auto-generated method stub

			final Map<RoadUser, Point> obs = rm.getObjectsAndPositions();

			for (final Entry<RoadUser, Point> entry : obs.entrySet()) {

				if (entry.getKey() instanceof RandomWalkAgent) {
					final int x = vp.toCoordX(entry.getValue().x);
					final int y = vp.toCoordY(entry.getValue().y);
					final int radius = 100;

					gc.setLineWidth(1);
					gc.setAlpha(30);
					gc.setBackground(new Color(gc.getDevice(), new RGB(150, 150, 150)));
					gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
					gc.setAlpha(255);
					gc.drawOval(x - radius, y - radius, radius * 2, radius * 2);
					gc.setAlpha(255);
				}
			}

			final RGB[] rgbs = new RGB[] { new RGB(51, 0, 153), new RGB(0, 165, 43), new RGB(255, 125, 41) };
			int i = 0;
			for (final Entry<RoadUser, Point> entry : obs.entrySet()) {

				if (entry.getKey() instanceof Package) {
					final int x = vp.toCoordX(entry.getValue().x);
					final int y = vp.toCoordY(entry.getValue().y);

					final List<RandomWalkAgent> list = RoadModels
							.findClosestObjects(entry.getValue(), rm, RandomWalkAgent.class, 3);

					for (final RandomWalkAgent rwa : list) {
						final Point p = rm.getPosition(rwa);
						final int x2 = vp.toCoordX(p.x);
						final int y2 = vp.toCoordY(p.y);

						gc.setAlpha(255);
						gc.setForeground(new Color(gc.getDevice(), rgbs[i % rgbs.length]));
						gc.setLineWidth(4);
						gc.drawLine(x, y, x2, y2);
						gc.setAlpha(255);
						// gc.fillOval(x - radius, y - radius, radius * 2,
						// radius * 2);
					}

				}

				i++;
			}

		}

		@Override
		public ViewRect getViewRect() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void registerModelProvider(ModelProvider mp) {
			rm = mp.getModel(RoadModel.class);
		}

	}
}
