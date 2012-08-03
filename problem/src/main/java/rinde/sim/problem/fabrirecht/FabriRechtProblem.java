/**
 * 
 */
package rinde.sim.problem.fabrirecht;

import static com.google.common.collect.Maps.newLinkedHashMap;

import java.util.Map;

import org.apache.commons.math3.random.MersenneTwister;

import rinde.sim.core.Simulator;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.core.model.road.AbstractRoadModel.RoadEvent;
import rinde.sim.core.model.road.MoveEvent;
import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.event.Event;
import rinde.sim.event.Listener;
import rinde.sim.scenario.ScenarioController;
import rinde.sim.scenario.TimedEvent;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class FabriRechtProblem extends ScenarioController {

	protected final FabriRechtScenario fabriRechtScenario;
	protected final StatisticsListener statisticsListener;

	/**
	 * @param scen
	 * @param numberOfTicks
	 */
	public FabriRechtProblem(FabriRechtScenario scen) {
		super(scen, (int) (scen.timeWindow.end - scen.timeWindow.begin));
		System.out.println("events: " + scen.asList());
		fabriRechtScenario = scen;
		statisticsListener = new StatisticsListener();
	}

	// subclasses can override this method to add more models
	@Override
	protected Simulator createSimulator() throws Exception {
		final Simulator sim = new Simulator(new MersenneTwister(123), 1);
		final RoadModel rm = new PlaneRoadModel(fabriRechtScenario.min, fabriRechtScenario.max, false, 1.0);
		rm.getEventAPI().addListener(statisticsListener, RoadEvent.MOVE);
		sim.register(rm);
		sim.register(new PDPModel(rm));
		return sim;
	}

	@Override
	protected final boolean handleTimedEvent(TimedEvent event) {
		if (event.getEventType() == PDPScenarioEvent.ADD_PARCEL) {
			return handleAddParcel(((AddParcelEvent) event));
		} else if (event.getEventType() == PDPScenarioEvent.ADD_VEHICLE) {
			return handleAddVehicle((AddVehicleEvent) event);
		} else if (event.getEventType() == PDPScenarioEvent.ADD_DEPOT) {
			return handleAddDepot((AddDepotEvent) event);
		}
		return false;
	}

	protected abstract boolean handleAddVehicle(AddVehicleEvent event);

	protected boolean handleAddParcel(AddParcelEvent event) {
		return getSimulator().register(new FRParcel(event.parcelDTO));
	}

	protected boolean handleAddDepot(AddDepotEvent event) {
		return getSimulator().register(new FRDepot(event.position));
	}

	class StatisticsListener implements Listener {

		protected final Map<MovingRoadUser, Double> distanceMap;
		protected double totalDistance;

		public StatisticsListener() {
			distanceMap = newLinkedHashMap();
			totalDistance = 0d;
		}

		@Override
		public void handleEvent(Event e) {
			final MoveEvent me = ((MoveEvent) e);
			increment(me.roadUser, me.pathProgress.distance);
			totalDistance += me.pathProgress.distance;
		}

		protected void increment(MovingRoadUser mru, double num) {
			if (!distanceMap.containsKey(mru)) {
				distanceMap.put(mru, num);
			} else {
				distanceMap.put(mru, distanceMap.get(mru) + num);
			}
		}
	}

}
