/**
 * 
 */
package rinde.sim.core.model.pdp;

import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.RoadModel;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class Truck implements PackageContainer, MovingRoadUser,
        TickListener {

    protected PDPModel pdpModel;
    protected RoadModel roadModel;

    private final Point startPosition;

    enum State {
        DRIVING, WAITING, LOADING, UNLOADING
    }

    public Truck(Point startPos) {
        startPosition = startPos;
    }

    @Override
    public final PDPType getType() {
        return PDPType.TRUCK;
    }

    @Override
    public final void tick(TimeLapse time) {
        // finish previously started pickup and delivery actions that need to
        // consume time
        pdpModel.continuePreviousActions(this, time);

        controlLoop(time);
    }

    // TODO is this a good idea?? might make this object way too complicated
    public final void pickup(Package p, TimeLapse time) {
        pdpModel.pickup(this, p, time);
    }

    @Override
    public final void initPDPObject(PDPModel model) {
        pdpModel = model;
    }

    @Override
    public final void initRoadUser(RoadModel model) {
        roadModel = model;
        roadModel.addObjectAt(this, startPosition);
    }

    protected abstract void controlLoop(TimeLapse time);

}
