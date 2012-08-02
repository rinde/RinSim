/**
 * 
 */
package rinde.sim.problem.fabrirecht;

import org.apache.commons.math3.random.MersenneTwister;

import rinde.sim.core.Simulator;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.scenario.ScenarioController;
import rinde.sim.scenario.TimedEvent;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class FabriRechtProblem extends ScenarioController {

	protected final FabriRechtScenario fabriRechtScenario;

	/**
	 * @param scen
	 * @param numberOfTicks
	 */
	public FabriRechtProblem(FabriRechtScenario scen) {
		super(scen, (int) (scen.timeWindow.end - scen.timeWindow.begin));
		System.out.println("events: " + scen.asList());
		fabriRechtScenario = scen;
	}

	// subclasses can override this method to add more models
	@Override
	protected Simulator createSimulator() throws Exception {
		final Simulator sim = new Simulator(new MersenneTwister(123), 1);
		final RoadModel rm = new PlaneRoadModel(fabriRechtScenario.min, fabriRechtScenario.max,
				Double.POSITIVE_INFINITY);
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

}
