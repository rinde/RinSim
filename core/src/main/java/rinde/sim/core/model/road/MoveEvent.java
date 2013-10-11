/**
 * 
 */
package rinde.sim.core.model.road;

import org.apache.commons.lang3.builder.ToStringBuilder;

import rinde.sim.core.model.road.AbstractRoadModel.RoadEventType;
import rinde.sim.event.Event;

/**
 * Event representing a move of a {@link MovingRoadUser}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class MoveEvent extends Event {
  private static final long serialVersionUID = 5118819834474109451L;

  /**
   * The {@link RoadModel} that dispatched this event.
   */
  public final RoadModel roadModel;

  /**
   * The {@link MovingRoadUser} that moved.
   */
  public final MovingRoadUser roadUser;

  /**
   * Object containing the distance, time and path of this move.
   */
  public final MoveProgress pathProgress;

  MoveEvent(RoadModel rm, MovingRoadUser ru, MoveProgress pp) {
    super(RoadEventType.MOVE, rm);
    roadModel = rm;
    roadUser = ru;
    pathProgress = pp;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
