package rinde.sim.lab.session2.example1;

import org.apache.commons.math.random.MersenneTwister;
import org.eclipse.swt.graphics.RGB;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultiAttributeEdgeData;
import rinde.sim.core.model.road.GraphRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.event.Event;
import rinde.sim.scenario.ConfigurationException;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.ScenarioController;
import rinde.sim.serializers.DotGraphSerializer;
import rinde.sim.serializers.SelfCycleFilter;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.ObjectRenderer;
import rinde.sim.ui.renderers.UiSchema;

public class SimpleController extends ScenarioController {

	String map;
	private RoadModel roadModel;

	public SimpleController(Scenario scen, int numberOfTicks, String map) throws ConfigurationException {
		super(scen, numberOfTicks);
		this.map = map;

		initialize();
	}

	@Override
	protected Simulator createSimulator() throws Exception {
		Graph<MultiAttributeEdgeData> graph;
		try {
			graph = DotGraphSerializer.getMultiAttributeGraphSerializer(new SelfCycleFilter()).read(map);
		} catch (Exception e) {
			throw new ConfigurationException("e:", e);
		}
		roadModel = new GraphRoadModel(graph);

		MersenneTwister rand = new MersenneTwister(123);
		Simulator s = new Simulator(rand, 10000);
		s.register(roadModel);
		return s;
	}

	@Override
	protected boolean createUserInterface() {
		UiSchema schema = new UiSchema();
		schema.add(RandomWalkAgent.class, new RGB(0, 0, 0));

		View.startGui(getSimulator(), 4, new ObjectRenderer(roadModel, schema, false));

		return true;
	}

	@Override
	protected boolean handleAddTruck(Event e) {
		RandomWalkAgent agent = new RandomWalkAgent(7, roadModel.getRandomPosition(getSimulator().getRandomGenerator()));
		getSimulator().register(agent);
		return true;
	}

}
