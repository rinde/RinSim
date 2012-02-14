/**
 * 
 */
package rinde.sim.examples.rwalk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.random.MersenneTwister;
import org.eclipse.swt.graphics.RGB;


import rinde.sim.core.RoadModel;
import rinde.sim.core.Simulator;
import rinde.sim.core.graph.MultimapGraph;
import rinde.sim.core.graph.Point;
import rinde.sim.serializers.DotGraphSerializer;
import rinde.sim.serializers.SelfCycleFilter;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.ObjectRenderer;
import rinde.sim.ui.renderers.UiSchema;
import rinde.sim.util.DotUtils;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class RandomWalkExample {

	public static void main(String[] args) throws Exception {
		// create a new simulator, load map of Leuven
		Simulator<RoadModel> simulator = new Simulator<RoadModel>(new RoadModel(new MultimapGraph()), new MersenneTwister(123), 1000);
		simulator.model.addGraph(new DotGraphSerializer(new SelfCycleFilter()).read("files/leuven.dot"));

		// create 300 agents, on random locations
		List<Point> nodes = new ArrayList<Point>(simulator.model.getNodes());
		for (int i = 0; i < 30; i++) {
			Point pos = nodes.get(simulator.rand.nextInt(nodes.size()));
			RandomWalkAgent agent = new RandomWalkAgent(simulator.model, simulator.rand);
			simulator.addTickListener(agent);
			simulator.model.addObjectAt(agent, pos);
		}

//		// GUI stuff: agents are red, packages are green
//		Map<Class<?>, RGB> colorMapping = new HashMap<Class<?>, RGB>();
//		colorMapping.put(RandomWalkAgent.class, new RGB(255, 0, 0));
//		colorMapping.put(Package.class, new RGB(0, 0, 255));
//		colorMapping.put(String.class, new RGB(0, 255, 0));
		
		UiSchema schema = new UiSchema();
//		schema.add(RandomWalkAgent.class, new RGB(255,0,0));
		schema.add(RandomWalkAgent.class, "/graphics/deliverytruck.png");
		schema.add(Package.class, "/graphics/flag.png");
//		schema.add(Package.class, new RGB(0x0,0x0,0xFF));
		
//		View.setTestingMode(true);
		View.startGui(simulator, 2, new ObjectRenderer(simulator.model, schema, false));
	}
}
