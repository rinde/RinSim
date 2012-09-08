/**
 * 
 */
package rinde.sim.examples.rwalk4;

import java.util.Random;

import org.apache.commons.math3.random.MersenneTwister;
import org.eclipse.swt.graphics.RGB;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.LengthData;
import rinde.sim.core.model.communication.CommunicationModel;
import rinde.sim.core.model.road.GraphRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.serializers.DotGraphSerializer;
import rinde.sim.serializers.SelfCycleFilter;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.GraphRoadModelRenderer;
import rinde.sim.ui.renderers.UiSchema;

/**
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public class RandomWalkExample {

	public static void main(String[] args) throws Exception {

		final String MAP_DIR = "../core/files/maps/";
		// create a new simulator, load map of Leuven
		MersenneTwister rand = new MersenneTwister(123);
		Simulator simulator = new Simulator(rand, 1000);
		Graph<LengthData> graph = DotGraphSerializer.getLengthGraphSerializer(new SelfCycleFilter()).read(MAP_DIR
				+ "leuven-simple.dot");
		// Graph<LengthEdgeData> graph =
		// DotGraphSerializer.getLengthGraphSerializer(new
		// SelfCycleFilter()).read("files/brussels.dot");
		RoadModel roadModel = new GraphRoadModel(graph);

		// XXX [bm] to be decided either Communication model have RG as a
		// constructor parameter or implements Simulator user interface
		CommunicationModel communicationModel = new CommunicationModel(rand, false);
		simulator.register(roadModel);
		simulator.register(communicationModel);

		simulator.configure();

		Random r = new Random(1317);
		for (int i = 0; i < 100; i++) {
			int radius = r.nextInt(300) + 200;
			double minSpeed = 50;
			double maxSpeed = 100;

			RandomWalkAgent agent = new RandomWalkAgent(minSpeed + (maxSpeed - minSpeed) * r.nextDouble(), radius,
					0.01 + r.nextDouble() / 2);
			simulator.register(agent);
		}

		// // GUI stuff: agents are red, packages are blue or have ico
		// represenation
		UiSchema schema = new UiSchema(false);
		// schema.add(RandomWalkAgent.class, new RGB(255,0,0));
		schema.add(rinde.sim.examples.common.Package.class, new RGB(0x0, 0x0, 0xFF));

		UiSchema schema2 = new UiSchema();
		schema2.add(RandomWalkAgent.C_BLACK, new RGB(0, 0, 0));
		schema2.add(RandomWalkAgent.C_YELLOW, new RGB(0xff, 0, 0));
		schema2.add(RandomWalkAgent.C_GREEN, new RGB(0x0, 0x80, 0));

		View.startGui(simulator, 4, new GraphRoadModelRenderer(), new RoadUserRenderer(schema, false), new MessagingLayerRenderer(
				roadModel, schema2));
	}
}
