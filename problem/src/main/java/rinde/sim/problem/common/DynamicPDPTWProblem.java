/**
 * 
 */
package rinde.sim.problem.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableMap;
import static rinde.sim.core.model.pdp.PDPScenarioEvent.TIME_OUT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.random.MersenneTwister;
import org.eclipse.swt.graphics.RGB;

import rinde.sim.core.Simulator;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.pdp.Depot;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.pdp.Vehicle;
import rinde.sim.core.model.pdp.twpolicy.TardyAllowedPolicy;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.problem.common.StatsTracker.StatisticsDTO;
import rinde.sim.problem.gendreau06.Gendreau06Scenario;
import rinde.sim.scenario.ScenarioController;
import rinde.sim.scenario.ScenarioController.UICreator;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.scenario.TimedEventHandler;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.PDPModelRenderer;
import rinde.sim.ui.renderers.PlaneRoadModelRenderer;
import rinde.sim.ui.renderers.Renderer;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.UiSchema;

/**
 * A problem instance for the class of problems which is called dynamic
 * pickup-and-delivery problems with time windows, often abbreviated as dynamic
 * PDPTW.
 * <p>
 * A problem instance is an instance which sets up everything related to the
 * 'problem' which one tries to solve. The idea is that a user only needs to
 * worry about adding its own solution to this instance.
 * <p>
 * By default this class needs very little customization, it needs to be given a
 * scenario which it then uses to configure the simulation. Further it is
 * required to plug your own vehicle in by using
 * {@link #addCreator(Class, Creator)}. Optionally this method can also be used
 * to plug custom parcels and depots in.
 * <p>
 * Currently the Gendreau et al. (2006) benchmark is supported. In the future
 * this class will also support the Fabri & Recht and Pankratz benchmarks.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class DynamicPDPTWProblem {

	/**
	 * A map which contains the default {@link Creator}s.
	 */
	protected static final Map<Class<?>, Creator<?>> DEFAULT_EVENT_CREATOR_MAP = initDefaultEventCreatorMap();

	/**
	 * Map containing the {@link Creator}s which handle specific
	 * {@link TimedEvent}s.
	 */
	protected final Map<Class<?>, Creator<?>> eventCreatorMap;

	/**
	 * The {@link ScenarioController} which is used to play the scenario.
	 */
	protected final ScenarioController controller;

	/**
	 * The {@link Simulator} which is used for the simulation.
	 */
	protected final Simulator simulator;

	/**
	 * The {@link UICreator} which is used for creating the default UI.
	 */
	protected final DefaultUICreator defaultUICreator;

	/**
	 * The {@link StatsTracker} which is used internally for gathering
	 * statistics.
	 */
	protected final StatsTracker statsTracker;

	/**
	 * The {@link TimeOutHandler} which is used to handle
	 * {@link rinde.sim.core.model.pdp.PDPScenarioEvent#TIME_OUT} events.
	 */
	protected TimeOutHandler timeOutHandler;

	/**
	 * Create a new problem instance using the specified scenario.
	 * @param scen The the {@link DynamicPDPTWScenario} which is used in this
	 *            problem.
	 * @param randomSeed The random seed which will be passed into the random
	 *            number generator in the simulator.
	 * @param models An optional list of models which can be added, with this
	 *            option custom models for specific solutions can be added.
	 */
	public DynamicPDPTWProblem(DynamicPDPTWScenario scen, long randomSeed, Model<?>... models) {
		simulator = new Simulator(new MersenneTwister(randomSeed), scen.getTickSize());
		// TODO speed converter needs to depend on scenario
		// TODO road model needs to depend on scenario
		simulator.register(new PlaneRoadModel(scen.getMin(), scen.getMax(), scen instanceof Gendreau06Scenario, scen
				.getMaxSpeed()));
		simulator.register(new PDPModel(new TardyAllowedPolicy()));
		for (final Model<?> m : models) {
			simulator.register(m);
		}
		eventCreatorMap = newHashMap();

		timeOutHandler = new TimeOutHandler() {
			@Override
			public void handleTimeOut(Simulator sim) {
				sim.stop();
			}
		};

		final TimedEventHandler handler = new TimedEventHandler() {

			@SuppressWarnings("unchecked")
			@Override
			public boolean handleTimedEvent(TimedEvent event) {
				if (eventCreatorMap.containsKey(event.getClass())) {
					return ((Creator<TimedEvent>) eventCreatorMap.get(event.getClass())).create(simulator, event);
				} else if (DEFAULT_EVENT_CREATOR_MAP.containsKey(event.getClass())) {
					return ((Creator<TimedEvent>) DEFAULT_EVENT_CREATOR_MAP.get(event.getClass()))
							.create(simulator, event);
				} else if (event.getEventType() == TIME_OUT) {
					timeOutHandler.handleTimeOut(simulator);
					return true;
				}
				return false;
			}
		};
		final int ticks = scen.getTimeWindow().end == Long.MAX_VALUE ? -1 : (int) (scen.getTimeWindow().end - scen
				.getTimeWindow().begin);
		controller = new ScenarioController(scen, simulator, handler, ticks);

		statsTracker = new StatsTracker(controller, simulator);
		defaultUICreator = new DefaultUICreator();
	}

	public StatisticsDTO getStatistics() {
		return statsTracker.getStatsDTO();
	}

	/**
	 * Enables UI using a default visualization.
	 */
	public void enableUI() {
		enableUI(defaultUICreator);
	}

	/**
	 * Allows to add an additional {@link Renderer} to the default UI.
	 * @param r The {@link Renderer} to add.
	 */
	public void addRendererToUI(Renderer r) {
		defaultUICreator.addRenderer(r);
	}

	/**
	 * Enables UI by allowing plugging in a custom {@link UICreator}.
	 * @param creator The creator to use.
	 */
	public void enableUI(UICreator creator) {
		controller.enableUI(creator);
	}

	/**
	 * Executes a simulation of the problem.
	 */
	public void simulate() {
		checkState(eventCreatorMap.containsKey(AddVehicleEvent.class), "A creator for AddVehicleEvent is required, use addCreator(..)");
		controller.start();
	}

	public Simulator getSimulator() {
		return simulator;
	}

	/**
	 * Using this method a {@link Creator} instance can be associated with a
	 * certain event. The creator will be called when the event is issued, it is
	 * the responsibility of the {@link Creator} the create the apropriate
	 * response. This method will override a previously existing creator for the
	 * specified event type if applicable.
	 * @param eventType The event type to which the creator will be associated.
	 * @param creator The creator that will be used.
	 */
	public <T extends TimedEvent> void addCreator(Class<T> eventType, Creator<T> creator) {
		checkArgument(eventType == AddVehicleEvent.class || eventType == AddParcelEvent.class
				|| eventType == AddDepotEvent.class, "A creator can only be added to one of the following classes: AddVehicleEvent, AddParcelEvent, AddDepotEvent.");
		eventCreatorMap.put(eventType, creator);
	}

	/**
	 * With this method the {@link TimeOutHandler} which is used can be changed.
	 * @param toh The time out handler to use.
	 */
	public void setTimeOutHandler(TimeOutHandler toh) {
		timeOutHandler = toh;
	}

	static Map<Class<?>, Creator<?>> initDefaultEventCreatorMap() {
		final Map<Class<?>, Creator<?>> map = newHashMap();
		map.put(AddParcelEvent.class, new Creator<AddParcelEvent>() {
			@Override
			public boolean create(Simulator sim, AddParcelEvent event) {
				return sim.register(new DefaultParcel(event.parcelDTO));
			}
		});
		map.put(AddDepotEvent.class, new Creator<AddDepotEvent>() {
			@Override
			public boolean create(Simulator sim, AddDepotEvent event) {
				return sim.register(new DefaultDepot(event.position));
			}
		});
		return unmodifiableMap(map);
	}

	// factory method for dealing with scenario events, note that the created
	// object must be registered to the simulator, not returned
	public interface Creator<T extends TimedEvent> {

		boolean create(Simulator sim, T event);
	}

	public interface TimeOutHandler {
		void handleTimeOut(Simulator sim);
	}

	class DefaultUICreator implements UICreator {
		protected List<Renderer> renderers;

		public DefaultUICreator() {
			final UiSchema schema = new UiSchema(false);
			schema.add(Vehicle.class, new RGB(255, 0, 0));
			schema.add(Depot.class, new RGB(0, 255, 0));
			schema.add(Parcel.class, new RGB(0, 0, 255));
			renderers = new ArrayList<Renderer>(asList(new PlaneRoadModelRenderer(40), new RoadUserRenderer(schema,
					false), new PDPModelRenderer()));
		}

		@Override
		public void createUI(Simulator sim) {
			View.startGui(sim, 1, renderers.toArray(new Renderer[] {}));
		}

		public void addRenderer(Renderer r) {
			renderers.add(r);
		}
	}
}
