package rinde.sim.lab.session1.example1;


import org.apache.commons.math.random.MersenneTwister;
import org.eclipse.swt.graphics.RGB;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultiAttributeEdgeData;
import rinde.sim.core.model.RoadModel;
import rinde.sim.serializers.DotGraphSerializer;
import rinde.sim.serializers.SelfCycleFilter;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.ObjectRenderer;
import rinde.sim.ui.renderers.UiSchema;

/**
 * 
 */
public class Example {

	public static void main(String[] args) throws Exception {
		final String MAP_DIR = "../core/files/maps/";
		MersenneTwister rand = new MersenneTwister(123);
		Simulator simulator = new Simulator(rand, 1000);
		Graph<MultiAttributeEdgeData> graph = DotGraphSerializer.getMultiAttributeGraphSerializer(new SelfCycleFilter()).read(MAP_DIR + "leuven-simple.dot");
		RoadModel roadModel = new RoadModel(graph);
		
		simulator.register(roadModel);	
		
		//once configured, no more models can be registered
		//simulation entities can only be registered after the simulator is configured
		simulator.configure();
	
		//create and register a number of RandomWalkAgents
		for (int i = 0; i < 30; i++) {
			RandomWalkAgent agent = new RandomWalkAgent(1, graph.getRandomNode(rand));
			simulator.register(agent);
		}
		
		UiSchema schema = new UiSchema();
		//display RandomWalkAgents in red
		schema.add(RandomWalkAgent.class, new RGB(255,0,0));

		//schema.add(RandomWalkAgent.class, "/graphics/deliverytruck.png");
	
		//start a gui with the simple objects renderer
		View.startGui(simulator, 5, new ObjectRenderer(roadModel, schema, false));
	}
}
