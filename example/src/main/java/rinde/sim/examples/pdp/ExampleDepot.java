/**
 * 
 */
package rinde.sim.examples.pdp;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.Depot;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.road.RoadModel;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class ExampleDepot extends Depot {

  public ExampleDepot(Point position, double capacity) {
    setStartPosition(position);
    setCapacity(capacity);
  }

  @Override
  public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

}
