package rinde.sim.examples.rwalk5;

import java.util.Random;

import org.apache.commons.math3.random.MersenneTwister;
import org.eclipse.swt.graphics.RGB;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultiAttributeData;
import rinde.sim.core.model.communication.CommunicationModel;
import rinde.sim.core.model.communication.CommunicationModel2;
import rinde.sim.core.model.road.GraphRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.scenario.ConfigurationException;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.ScenarioController;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.serializers.DotGraphSerializer;
import rinde.sim.serializers.SelfCycleFilter;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.RoadsRenderer;
import rinde.sim.ui.renderers.UiSchema;

public class SimpleController extends ScenarioController {

	private final StatisticsCollector statistics = new StatisticsCollector();

	private RoadModel roadModel;
	private Random randomizer;

	private final String map;

	/**
	 * Simple controller
	 * @param scen scenario to realize
	 * @param map name of the file with a map
	 * @throws ConfigurationException
	 */
	public SimpleController(Scenario scen, String map) throws ConfigurationException {
		super(scen, -1);

		this.map = map;

		// MUST be called !!!
		initialize();
	}

	/**
	 * the method is called as part of initialize method
	 */
	@Override
	protected Simulator createSimulator() throws Exception {
		randomizer = new Random(1317);
		Graph<MultiAttributeData> graph;
		try {
			graph = DotGraphSerializer.getMultiAttributeGraphSerializer(new SelfCycleFilter()).read(map);
		} catch (final Exception e) {
			// not the smartest error handling
			throw new ConfigurationException("e:", e);
		}
		roadModel = new GraphRoadModel(graph);

		final MersenneTwister rand = new MersenneTwister(123);
		final Simulator s = new Simulator(rand, 10000);
		final CommunicationModel communicationModel = new CommunicationModel2(rand);
		s.register(roadModel);
		s.register(communicationModel);
		return s;
	}

	/**
	 * Create user interface for the simulation
	 * @see rinde.sim.scenario.ScenarioController#createUserInterface()
	 */
	@Override
	protected boolean createUserInterface() {
		final UiSchema schema = new UiSchema(false);
		schema.add(rinde.sim.examples.common.Package.class, new RGB(0x0, 0x0, 0xFF));

		final UiSchema schema2 = new UiSchema();
		schema2.add(RandomWalkAgent.C_BLACK, new RGB(0, 0, 0));
		schema2.add(RandomWalkAgent.C_YELLOW, new RGB(0xff, 0, 0));
		schema2.add(RandomWalkAgent.C_GREEN, new RGB(0x0, 0x80, 0));

		View.startGui(getSimulator(), 4, new RoadsRenderer(), new RoadUserRenderer(schema, false), new MessagingLayerRenderer(
				roadModel, schema2));

		return true;
	}

	@Override
	protected boolean handleTimedEvent(TimedEvent event) {
		if (event.getEventType() == ScenarioEvent.ADD_TRUCK) {
			final int radius = randomizer.nextInt(300) + 200;

			final double minSpeed = 30;
			final double maxSpeed = 140;
			final RandomWalkAgent agent = new RandomWalkAgent(minSpeed + (maxSpeed - minSpeed)
					* randomizer.nextDouble(), radius, 0.01 + randomizer.nextDouble() / 2);
			getSimulator().register(agent);
			agent.eventAPI.addListener(statistics, RandomWalkAgent.Type.FINISHED_SERVICE);
			// it is important to inform controller that this event was handled
			// to avoid runtime exceptions
			return true;
		}
		// likewise, it is important to inform the controller that other
		// events are *not* handled, this ensures fail-fast behavior in case the
		// scenario contains events which were not anticipated.
		return false;
	}
}
