/**
 * 
 */
package rinde.sim.pdptw.common;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.scenario.TimedEvent;

/**
 * Event indicating that a depot can be created.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class AddDepotEvent extends TimedEvent {
  private static final long serialVersionUID = -7517322583609266323L;

  /**
   * The position where the depot is to be added.
   */
  public final Point position;

  /**
   * Create a new instance.
   * @param time The time at which the event is to be dispatched.
   * @param pPosition {@link #position}
   */
  public AddDepotEvent(long time, Point pPosition) {
    super(PDPScenarioEvent.ADD_DEPOT, time);
    position = pPosition;
  }

}
