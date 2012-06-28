package rinde.sim.lab.session1.catch_the_flag_solution;

import org.apache.commons.math3.random.MersenneTwister;
import org.eclipse.swt.graphics.RGB;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultiAttributeEdgeData;
import rinde.sim.core.model.communication.CommunicationModel;
import rinde.sim.core.model.road.GraphRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.lab.common.Flag;
import rinde.sim.serializers.DotGraphSerializer;
import rinde.sim.serializers.SelfCycleFilter;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.RoadsRenderer;
import rinde.sim.ui.renderers.UiSchema;

/**
 * 
 */
public class CatchTheFlag {

	public static void main(String[] args) throws Exception {
		final String MAP_DIR = "../core/files/maps/";
		MersenneTwister rand = new MersenneTwister(123);
		Simulator simulator = new Simulator(rand, 1000);
		Graph<MultiAttributeEdgeData> graph = DotGraphSerializer
				.getMultiAttributeGraphSerializer(new SelfCycleFilter()).read(MAP_DIR + "leuven-simple.dot");

		RoadModel roadModel = new GraphRoadModel(graph);
		simulator.register(roadModel);

		// Create and register a new communication model
		CommunicationModel communicationModel = new CommunicationModel(rand, true);
		simulator.register(communicationModel);

		simulator.configure();

		for (int i = 0; i < 5; i++) {
			TruckAgent agent = new TruckAgent("Truck" + i, 20, graph.getRandomNode(rand));
			simulator.register(agent);
		}

		DepotAgent depotAgent = new DepotAgent(graph.getRandomNode(rand));
		simulator.register(depotAgent);

		UiSchema schema = new UiSchema();
		schema.add(TruckAgent.class, new RGB(255, 0, 0));
		schema.add(DepotAgent.class, new RGB(0, 0, 255));
		schema.add(Flag.class, "/graphics/flag.png");

		View.startGui(simulator, 5, new RoadsRenderer(), new RoadUserRenderer(schema, false));
	}
}
