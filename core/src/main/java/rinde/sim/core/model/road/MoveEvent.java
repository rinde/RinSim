/**
 * 
 */
package rinde.sim.core.model.road;

import org.apache.commons.lang3.builder.ToStringBuilder;

import rinde.sim.core.model.road.AbstractRoadModel.RoadEventType;
import rinde.sim.event.Event;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class MoveEvent extends Event {

    public final RoadModel roadModel;
    public final MovingRoadUser roadUser;
    public final MoveProgress pathProgress;

    /**
     */
    public MoveEvent(RoadModel rm, MovingRoadUser ru, MoveProgress pp) {
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
