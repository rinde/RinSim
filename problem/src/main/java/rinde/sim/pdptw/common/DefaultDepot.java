/**
 * 
 */
package rinde.sim.pdptw.common;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.Depot;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.road.RoadModel;

/**
 * A default {@link Depot} implementation, it does nothing.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class DefaultDepot extends Depot {

  /**
   * Instantiate the depot at the provided position.
   * @param startPosition The position where the depot will be located.
   */
  public DefaultDepot(Point startPosition) {
    setStartPosition(startPosition);
  }

  @Override
  public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}
}
