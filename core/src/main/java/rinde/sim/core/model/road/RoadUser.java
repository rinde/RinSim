/** 
 * 
 */
package rinde.sim.core.model.road;

/**
 * A RoadUser is an object living on the {@link RoadModel}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface RoadUser {
  /**
   * This is called by {@link RoadModel#register(RoadUser)}.
   * @param model The model on which this RoadUser is registered.
   * @see RoadModel#register(RoadUser)
   * @see RoadModel#unregister(RoadUser)
   */
  void initRoadUser(RoadModel model);
}
