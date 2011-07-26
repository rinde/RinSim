/**
 * 
 */
package rinde.sim.examples.rwalk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.random.MersenneTwister;
import org.eclipse.swt.graphics.RGB;

import rinde.sim.core.Point;
import rinde.sim.core.RoadStructure;
import rinde.sim.core.Simulator;
import rinde.sim.core.graph.MultimapGraph;
import rinde.sim.ui.ObjectRenderer;
import rinde.sim.ui.View;
import rinde.sim.util.DotUtils;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class RandomWalkExample {

	public static void main(String[] args) throws Exception {
		// create a new simulator, load map of Leuven
		Simulator<RoadStructure> simulator = new Simulator<RoadStructure>(new RoadStructure(new MultimapGraph()), new MersenneTwister(123), 1000);
		simulator.model.addGraph(DotUtils.parseDot("files/maps/dot/leuven.dot"));

		// create 300 agents, on random locations
		List<Point> nodes = simulator.model.getNodes();
		for (int i = 0; i < 300; i++) {
			Point pos = nodes.get(simulator.rand.nextInt(nodes.size()));
			RandomWalkAgent agent = new RandomWalkAgent(simulator.model, simulator.rand);
			simulator.addTickListener(agent);
			simulator.model.addObjectAt(agent, pos);
		}

		// GUI stuff: agents are red, packages are green
		Map<Class<?>, RGB> colorMapping = new HashMap<Class<?>, RGB>();
		colorMapping.put(RandomWalkAgent.class, new RGB(255, 0, 0));
		colorMapping.put(String.class, new RGB(0, 255, 0));
		View.startGui(simulator, 1, new ObjectRenderer(simulator.model, colorMapping, false));
	}
}
