/**
 * 
 */
package rinde.sim.core.model.pdp;

import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.model.road.MovingRoadUser;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class Truck extends ContainerImpl implements
        MovingRoadUser, TickListener {

    enum State {
        DRIVING, WAITING, LOADING, UNLOADING
    }

    @Override
    public final PDPType getType() {
        return PDPType.TRUCK;
    }

    @Override
    public final void tick(TimeLapse time) {
        // finish previously started pickup and delivery actions that need to
        // consume time
        getPDPModel().continuePreviousActions(this, time);

        tickImpl(time);
    }

    protected abstract void tickImpl(TimeLapse time);
}
