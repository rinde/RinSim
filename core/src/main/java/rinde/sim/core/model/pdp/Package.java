/**
 * 
 */
package rinde.sim.core.model.pdp;

import rinde.sim.core.graph.Point;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class Package implements PDPObject {

    protected final int loadingDuration;
    protected final int unloadingDuration;
    protected final Point destination;

    public Package(Point pDestination, int pLoadingDuration,
            int pUnloadingDuration) {
        destination = pDestination;
        loadingDuration = pLoadingDuration;
        unloadingDuration = pUnloadingDuration;
    }

    @Override
    public final PDPType getType() {
        return PDPType.PACKAGE;
    }

    // indicates 'size'/heaviness/etc
    abstract double getMagnitude();

    // time needed for pickup
    public int getLoadingDuration() {
        return loadingDuration;
    }

    // time needed for delivery
    public int getUnloadingDuration() {
        return unloadingDuration;
    }

    public Point getDestination() {
        return destination;
    }

}
