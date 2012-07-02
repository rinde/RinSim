/**
 * 
 */
package rinde.sim.core.model.pdp;

import rinde.sim.core.model.road.MovingRoadUser;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public abstract class Truck implements PackageContainer, MovingRoadUser {

    @Override
    public final PDPType getType() {
        return PDPType.TRUCK;
    }

}
