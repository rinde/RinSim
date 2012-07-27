package rinde.sim.lab.session2.gradient_field_exercise;

import org.apache.commons.math3.random.MersenneTwister;
import org.eclipse.swt.graphics.RGB;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultiAttributeData;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.GraphRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.lab.session2.gradient_field_exercise.packages.DeliveryLocation;
import rinde.sim.lab.session2.gradient_field_exercise.packages.Package;
import rinde.sim.lab.session2.gradient_field_exercise.packages.PackageAgent;
import rinde.sim.lab.session2.gradient_field_exercise.trucks.Truck;
import rinde.sim.lab.session2.gradient_field_exercise.trucks.TruckAgent;
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

	String map;

	private RoadModel roadModel;

	private int truckID = 0;
	private int packageID = 0;
	private Graph<MultiAttributeData> graph;

	public SimpleController(Scenario scen, int numberOfTicks, String map) throws ConfigurationException {
		super(scen, numberOfTicks);
		this.map = map;

		initialize();
	}

	@Override
	protected Simulator createSimulator() throws Exception {
		try {
			graph = DotGraphSerializer.getMultiAttributeGraphSerializer(new SelfCycleFilter()).read(map);
		} catch (final Exception e) {
			throw new ConfigurationException("e:", e);
		}
		roadModel = new GraphRoadModel(graph);

		final MersenneTwister rand = new MersenneTwister(123);
		final Simulator s = new Simulator(rand, 10000);
		s.register(roadModel);
		return s;
	}

	@Override
	protected boolean createUserInterface() {
		final UiSchema schema = new UiSchema();
		schema.add(Truck.class, new RGB(0, 0, 255));
		schema.add(Package.class, new RGB(255, 0, 0));
		schema.add(DeliveryLocation.class, new RGB(0, 255, 0));

		View.startGui(getSimulator(), 3, new RoadsRenderer(), new RoadUserRenderer(schema, false));

		return true;
	}

	@Override
	protected boolean handleTimedEvent(TimedEvent event) {

		if (event.getEventType() == ExampleEvent.ADD_TRUCK) {
			final Truck truck = new Truck("Truck-" + truckID++,
					graph.getRandomNode(getSimulator().getRandomGenerator()), 7);
			getSimulator().register(truck);
			final TruckAgent agent = new TruckAgent(truck, 5);
			getSimulator().register(agent);
			return true;
		} else if (event.getEventType() == ExampleEvent.ADD_PACKAGE) {
			final Point pl = graph.getRandomNode(getSimulator().getRandomGenerator());
			final DeliveryLocation dl = new DeliveryLocation(graph.getRandomNode(getSimulator().getRandomGenerator()));
			getSimulator().register(pl);
			getSimulator().register(dl);

			final Package p = new Package("Package-" + packageID++, pl, dl);
			getSimulator().register(p);
			final PackageAgent agent = new PackageAgent(p);
			getSimulator().register(agent);
			return true;
		}

		return false;
	}
}
