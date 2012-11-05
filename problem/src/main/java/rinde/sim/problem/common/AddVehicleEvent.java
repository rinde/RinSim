/**
 * 
 */
package rinde.sim.problem.common;

import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.scenario.TimedEvent;

/**
 * Event indicating that a vehicle can be created.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class AddVehicleEvent extends TimedEvent {

	private static final long serialVersionUID = 4424003258869579149L;
	public final VehicleDTO vehicleDTO;

	public AddVehicleEvent(long time, VehicleDTO dto) {
		super(PDPScenarioEvent.ADD_VEHICLE, time);
		vehicleDTO = dto;
	}

}
