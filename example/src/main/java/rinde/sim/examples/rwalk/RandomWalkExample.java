/**
 * 
 */
package rinde.sim.examples.rwalk;

import org.apache.commons.math.random.MersenneTwister;
import org.eclipse.swt.graphics.RGB;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.MultimapGraph;
import rinde.sim.core.model.RoadModel;
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
		// create a new simulator, load map of Leuven
		MersenneTwister rand = new MersenneTwister(123);
		Simulator simulator = new Simulator(rand, 100000);
		RoadModel roadModel = new RoadModel(new MultimapGraph());
		roadModel.addGraph(new DotGraphSerializer(new SelfCycleFilter()).read("files/leuven.dot"));
		
		simulator.register(roadModel);
		
		simulator.configure();

		for (int i = 0; i < 50; i++) {
			//FIXME to change the random generator
			RandomWalkAgent agent = new RandomWalkAgent(rand);
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
