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

        tickImpl(time);
    }

    @Override
    public final void initPDPObject(PDPModel model) {
        pdpModel = model;
        if (pdpModel != null && roadModel != null) {
            init();
        }
    }

    @Override
    public final void initRoadUser(RoadModel model) {
        roadModel = model;
        roadModel.addObjectAt(this, startPosition);
        if (pdpModel != null && roadModel != null) {
            init();
        }
    }

    protected abstract void tickImpl(TimeLapse time);

    // should perhapse not be abstract (not mandatory)
    protected abstract void init();

}
