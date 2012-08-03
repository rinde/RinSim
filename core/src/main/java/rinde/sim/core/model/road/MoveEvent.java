/**
 * 
 */
package rinde.sim.core.model.road;

import rinde.sim.core.model.road.AbstractRoadModel.RoadEvent;
import rinde.sim.event.Event;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class MoveEvent extends Event {

    public final RoadModel roadModel;
    public final RoadUser roadUser;
    public final MoveProgress pathProgress;

    /**
     */
    public MoveEvent(RoadModel rm, RoadUser ru, MoveProgress pp) {
        super(RoadEvent.MOVE, rm);
        roadModel = rm;
        roadUser = ru;
        pathProgress = pp;
    }

}
