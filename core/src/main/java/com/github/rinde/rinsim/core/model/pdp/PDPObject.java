/**
 * 
 */
package com.github.rinde.rinsim.core.model.pdp;

import com.github.rinde.rinsim.core.model.road.RoadUser;

/**
 * Base interface for objects in {@link PDPModel}. Can be used directly but
 * usually one of its subclasses are used instead:
 * <ul>
 * <li>{@link Vehicle}</li>
 * <li>{@link Parcel}</li>
 * <li>{@link Depot}</li>
 * </ul>
 * 
 * @author Rinde van Lon 
 */
public interface PDPObject extends RoadUser {

  /**
   * @return The type of the PDPObject.
   */
  PDPType getType();

  /**
   * Is called when object is registered in {@link PDPModel}.
   * @param model A reference to the model.
   */
  void initPDPObject(PDPModel model);

}
