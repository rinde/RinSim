/**
 * 
 */
package rinde.sim.core.model.pdp;

/**
 * PDP specific event type that may occur in a scenario.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public enum PDPScenarioEvent {

  /**
   * Indicates the arrival of a new vehicle.
   */
  ADD_VEHICLE,

  /**
   * Indicates the removal of a vehicle.
   */
  REMOVE_VEHICLE,

  /**
   * Indicates the arrival of a new parcel.
   */
  ADD_PARCEL,

  /**
   * Indicates the removal of a parcel.
   */
  REMOVE_PARCEL,

  /**
   * Indicates the arrival of a depot.
   */
  ADD_DEPOT,

  /**
   * Indicates the removal of a depot.
   */
  REMOVE_DEPOT,

  /**
   * Indicates the end of scenario time, e.g. this may be used to indicate the
   * end of a working day.
   */
  TIME_OUT;
}
