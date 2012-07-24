/**
 * 
 */
package rinde.sim.core.model.pdp;

import rinde.sim.core.graph.Point;

/**
 * The parcel class represents goods that can be transported.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public abstract class Parcel extends PDPObjectImpl {

    /**
     * The time it takes to load ('pickup') this parcel.
     */
    protected final int loadingDuration;
    /**
     * The time it takes to unload ('deliver') this parcel.
     */
    protected final int unloadingDuration;
    /**
     * The destination of this parcel, this is the position to where this parcel
     * needs to be delivered.
     */
    protected final Point destination;
    /**
     * The magnitude of this parcel, can be weight/volume/count depending on the
     * specific application.
     */
    protected final double magnitude;

    /**
     * Create a new parcel.
     * @param pDestination The position where this parcel needs to be delivered.
     * @param pLoadingDuration The time needed for pickup.
     * @param pUnloadingDuration The time needed for delivery.
     * @param pMagnitude The weight/volume/count of this parcel.
     */
    public Parcel(Point pDestination, int pLoadingDuration,
            int pUnloadingDuration, double pMagnitude) {
        destination = pDestination;
        loadingDuration = pLoadingDuration;
        unloadingDuration = pUnloadingDuration;
        magnitude = pMagnitude;
    }

    @Override
    public final PDPType getType() {
        return PDPType.PARCEL;
    }

    /**
     * @return {@link #magnitude}
     */
    public final double getMagnitude() {
        return magnitude;
    }

    /**
     * @return {@link #loadingDuration}
     */
    public final int getLoadingDuration() {
        return loadingDuration;
    }

    /**
     * @return {@link #unloadingDuration}
     */
    public final int getUnloadingDuration() {
        return unloadingDuration;
    }

    /**
     * @return {@link #destination}
     */
    public final Point getDestination() {
        return destination;
    }

}
