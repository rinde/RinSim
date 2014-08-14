/** 
 * 
 */
package com.github.rinde.rinsim.core.model.road;

/**
 * A RoadUser is an object living on the {@link RoadModel}.
 * @author Rinde van Lon
 */
public interface RoadUser {
  /**
   * This is called when an road user can initialize itself.
   * @param model The model on which this RoadUser is registered.
   */
  void initRoadUser(RoadModel model);
}
