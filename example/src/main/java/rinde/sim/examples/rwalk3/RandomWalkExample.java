/**
 * 
 */
package rinde.sim.examples.rwalk3;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultiAttributeData;
import rinde.sim.core.model.communication.CommunicationModel;
import rinde.sim.core.model.road.GraphRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.example.rwalk.common.Package;
import rinde.sim.serializers.DotGraphSerializer;
import rinde.sim.serializers.SelfCycleFilter;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.RoadsRenderer;
import rinde.sim.ui.renderers.UiSchema;

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
				.getMultiAttributeGraphSerializer(new SelfCycleFilter()).read(MAP_DIR + "leuven-simple.dot");
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
		for (int i = 0; i < 10; i++) {
			final RandomWalkAgent agent = new RandomWalkAgent(r.nextDouble() * 100);
			simulator.register(agent);
		}

		// // GUI stuff: agents are red, packages are blue or have ico
		// represenation
		final UiSchema schema = new UiSchema();
		// schema.add(RandomWalkAgent.class, new RGB(255, 0, 0));
		schema.add(RandomWalkAgent.class, "/graphics/deliverytruck.png");
		schema.add(Package.class, "/graphics/deliverypackage.png");
		// schema.add(Package.class, new RGB(0x0, 0x0, 0xFF));

		// View.setTestingMode(true);
		View.startGui(simulator, 5, new RoadsRenderer(20), new RoadUserRenderer(schema, false));
	}
}
