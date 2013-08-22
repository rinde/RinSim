/**
 * 
 */
package rinde.sim.pdptw.common;

import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.scenario.TimedEvent;

/**
 * Event indicating that a vehicle can be created.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class AddVehicleEvent extends TimedEvent {
  private static final long serialVersionUID = 4424003258869579149L;

  /**
   * Data which describes the vehicle that should be added.
   */
  public final VehicleDTO vehicleDTO;

  /**
   * New instance.
   * @param time {@link TimedEvent#time}
   * @param dto {@link #vehicleDTO}
   */
  public AddVehicleEvent(long time, VehicleDTO dto) {
    super(PDPScenarioEvent.ADD_VEHICLE, time);
    vehicleDTO = dto;
  }

}
