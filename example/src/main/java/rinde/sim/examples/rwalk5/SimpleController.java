package rinde.sim.examples.rwalk5;

import java.util.Random;

import org.apache.commons.math.random.MersenneTwister;
import org.eclipse.swt.graphics.RGB;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.LengthEdgeData;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.communication.CommunicationModel;
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

	private StatisticsCollector statistics = new StatisticsCollector();

	private RoadModel roadModel;
	private Random randomizer;
	
	
	/**
	 * Simple controller 
	 * @param scen scenario to realize
	 * @param map name of the file with a map
	 * @throws ConfigurationException
	 */
	public SimpleController(Scenario scen, String map) throws ConfigurationException {
		super(scen, -1);
		
		//configuration of the data that can be for example used in the createSimulator()
		randomizer = new Random(1317);
		Graph<LengthEdgeData> graph;
		try {
			graph = DotGraphSerializer.getLengthGraphSerializer(new SelfCycleFilter()).read(map);
		} catch (Exception e) {
			//not the smartes one
			throw new ConfigurationException("e:",e);
		}
		roadModel = new RoadModel(graph);
		
		
		
		//MUST be called !!!
		initialize();
	}
	
	/**
	 * the method is called as part of initialize method
	 */
	@Override
	protected Simulator createSimulator() throws Exception {
		
		MersenneTwister rand = new MersenneTwister(123);
		Simulator s = new Simulator(rand, 100);
		CommunicationModel communicationModel = new CommunicationModel(rand);
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
		UiSchema schema = new UiSchema(false);
		schema.add(rinde.sim.example.rwalk.common.Package.class, new RGB(0x0,0x0,0xFF));
		
		UiSchema schema2 = new UiSchema();
		schema2.add(RandomWalkAgent.C_BLACK, new RGB(0,0,0));
		schema2.add(RandomWalkAgent.C_YELLOW, new RGB(0xff,0,0));
		schema2.add(RandomWalkAgent.C_GREEN, new RGB(0x0,0x80,0));
		
		View.startGui(getSimulator(), 4, new ObjectRenderer(roadModel, schema, false), new MessagingLayerRenderer(roadModel, schema2) );
		
		return true;
	}
	
	/** 
	 * As the only event we need to handle is create the truck we handle it ;)
	 * The method have the same semantics like in earlier examples.
	 */
	@Override
	protected boolean handleAddTruck(Event e) {
		int radius = randomizer.nextInt(300) + 200;
		double minSpeed = 0.0005; double maxSpeed = 0.002; 
		RandomWalkAgent agent = new RandomWalkAgent(minSpeed + (maxSpeed - minSpeed) * randomizer.nextDouble(), radius, 0.01 + randomizer.nextDouble() / 2);
		getSimulator().register(agent);
		agent.addListener(statistics, RandomWalkAgent.Type.FINISHED_SERVICE);
		//it is important to inform controller that event was handled to aviod runtime exception
		return true;
	}

}
