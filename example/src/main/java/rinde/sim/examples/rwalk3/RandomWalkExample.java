/**
 * 
 */
package rinde.sim.examples.rwalk3;

import java.util.Random;

import org.apache.commons.math.random.MersenneTwister;
import org.eclipse.swt.graphics.RGB;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultiAttributeEdgeData;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.communication.CommunicationModel;
import rinde.sim.example.rwalk.common.Package;
import rinde.sim.serializers.DotGraphSerializer;
import rinde.sim.serializers.SelfCycleFilter;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.ObjectRenderer;
import rinde.sim.ui.renderers.UiSchema;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class RandomWalkExample {

	public static void main(String[] args) throws Exception {
		
		final String MAP_DIR = "../core/files/maps/dot/";
		// create a new simulator, load map of Leuven
		MersenneTwister rand = new MersenneTwister(123);
		Simulator simulator = new Simulator(rand, 1000);
		Graph<MultiAttributeEdgeData> graph = DotGraphSerializer.getMultiAttributeGraphSerializer(new SelfCycleFilter()).read(MAP_DIR + "leuven.dot");
//		roadModel.addGraph(DotGraphSerializer.getLengthGraphSerializer(new SelfCycleFilter()).read("files/brussels.dot"));
		RoadModel roadModel = new RoadModel(graph);
		
		//XXX [bm] to be decided either Communication model have RG as a constructor parameter or implements Simulator user interface
		CommunicationModel communicationModel = new CommunicationModel(rand);
		simulator.register(roadModel);
		simulator.register(communicationModel);
		
		simulator.configure();

		Random r = new Random(1317);
		for (int i = 0; i < 100; i++) {
			RandomWalkAgent agent = new RandomWalkAgent(r.nextDouble() * 100);
			simulator.register(agent);
		}

//		// GUI stuff: agents are red, packages are blue or have ico represenation
		UiSchema schema = new UiSchema();
		schema.add(RandomWalkAgent.class, new RGB(255,0,0));
//		schema.add(RandomWalkAgent.class, "/graphics/deliverytruck.png");
//		schema.add(Package.class, "/graphics/flag.png");
		schema.add(Package.class, new RGB(0x0,0x0,0xFF));
		
//		View.setTestingMode(true);
		View.startGui(simulator, 5, new ObjectRenderer(roadModel, schema, false));
	}
}
