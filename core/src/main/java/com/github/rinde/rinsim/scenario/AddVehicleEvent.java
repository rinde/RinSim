/**
 * 
 */
package com.github.rinde.rinsim.scenario;

import com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent;
import com.github.rinde.rinsim.core.pdptw.VehicleDTO;

/**
 * Event indicating that a vehicle can be created.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class AddVehicleEvent extends TimedEvent {

  /**
   * Data which describes the vehicle that should be added.
   */
  public final VehicleDTO vehicleDTO;

  /**
   * New instance.
   * @param t {@link TimedEvent#time}
   * @param dto {@link #vehicleDTO}
   */
  public AddVehicleEvent(long t, VehicleDTO dto) {
    super(PDPScenarioEvent.ADD_VEHICLE, t);
    vehicleDTO = dto;
  }
}
