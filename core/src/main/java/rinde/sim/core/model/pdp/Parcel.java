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
     * The time it takes to pickup this parcel.
     */
    protected final int pickupDuration;
    /**
     * The time it takes to deliver this parcel.
     */
    protected final int deliveryDuration;
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
     * @param pPickupDuration The time needed for pickup.
     * @param pDeliveryDuration The time needed for delivery.
     * @param pMagnitude The weight/volume/count of this parcel.
     */
    public Parcel(Point pDestination, int pPickupDuration,
            int pDeliveryDuration, double pMagnitude) {
        destination = pDestination;
        pickupDuration = pPickupDuration;
        deliveryDuration = pDeliveryDuration;
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
     * @return {@link #pickupDuration}
     */
    public final int getPickupDuration() {
        return pickupDuration;
    }

    /**
     * @return {@link #deliveryDuration}
     */
    public final int getDeliveryDuration() {
        return deliveryDuration;
    }

    /**
     * @return {@link #destination}
     */
    public final Point getDestination() {
        return destination;
    }

}
