/**
 * 
 */
package rinde.sim.core.model.pdp;

import rinde.sim.core.graph.Point;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class Parcel extends PDPObjectImpl {

    protected final int loadingDuration;
    protected final int unloadingDuration;
    protected final Point destination;
    protected final double magnitude;

    public Parcel(Point pDestination, int pLoadingDuration,
            int pUnloadingDuration, double pMagnitude) {
        destination = pDestination;
        loadingDuration = pLoadingDuration;
        unloadingDuration = pUnloadingDuration;
        magnitude = pMagnitude;
    }

    @Override
    public final PDPType getType() {
        return PDPType.PACKAGE;
    }

    // indicates 'size'/heaviness/etc
    public final double getMagnitude() {
        return magnitude;
    }

    // time needed for pickup
    public final int getLoadingDuration() {
        return loadingDuration;
    }

    // time needed for delivery
    public final int getUnloadingDuration() {
        return unloadingDuration;
    }

    public final Point getDestination() {
        return destination;
    }

}
