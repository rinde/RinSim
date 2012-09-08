/**
 * 
 */
package rinde.sim.examples.pdp;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultiAttributeData;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.road.GraphRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.serializers.DotGraphSerializer;
import rinde.sim.serializers.SelfCycleFilter;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.GraphRoadModelRenderer;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.UiSchema;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class PDPExample {

	public static void main(String[] args) throws FileNotFoundException, IOException {

		final String MAP_DIR = "../core/files/maps/";
		// create a new simulator, load map of Leuven
		final RandomGenerator rng = new MersenneTwister(123);
		final Simulator simulator = new Simulator(rng, 1000);
		final Graph<MultiAttributeData> graph = DotGraphSerializer
				.getMultiAttributeGraphSerializer(new SelfCycleFilter()).read(MAP_DIR + "leuven-simple.dot");
		final RoadModel roadModel = new GraphRoadModel(graph);
		final PDPModel pdpModel = new PDPModel();
		simulator.register(roadModel);
		simulator.register(pdpModel);
		simulator.configure();

		for (int i = 0; i < 5; i++) {
			simulator.register(new ExampleDepot(roadModel.getRandomPosition(rng), 100));
		}

		for (int i = 0; i < 10; i++) {
			simulator.register(new ExampleTruck(roadModel.getRandomPosition(rng), 10));
		}

		for (int i = 0; i < 30; i++) {
			simulator.register(new ExampleParcel(roadModel.getRandomPosition(rng), roadModel.getRandomPosition(rng),
					10, 10, 10.0));
		}

		final UiSchema uis = new UiSchema();
		uis.add(ExampleDepot.class, "/graphics/perspective/tall-building-64.png");
		uis.add(ExampleTruck.class, "/graphics/flat/taxi-32.png");
		uis.add(ExampleParcel.class, "/graphics/flat/hailing-cab-32.png");
		View.startGui(simulator, 1, new GraphRoadModelRenderer(), new RoadUserRenderer(uis, false));

	}
}
