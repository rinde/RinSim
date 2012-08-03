/**
 * 
 */
package rinde.sim.core.model.pdp;

import rinde.sim.core.graph.Point;
import rinde.sim.util.TimeWindow;

/**
 * The parcel class represents goods that can be transported.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public abstract class Parcel extends PDPObjectImpl {

    /**
     * The time it takes to pickup this parcel.
     */
    protected final long pickupDuration;
    protected final TimeWindow pickupTimeWindow;
    /**
     * The time it takes to deliver this parcel.
     */
    protected final long deliveryDuration;
    protected final TimeWindow deliveryTimeWindow;
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
    public Parcel(Point pDestination, long pPickupDuration,
            TimeWindow pickupTW, long pDeliveryDuration, TimeWindow deliveryTW,
            double pMagnitude) {
        destination = pDestination;
        pickupDuration = pPickupDuration;
        pickupTimeWindow = pickupTW;
        deliveryDuration = pDeliveryDuration;
        deliveryTimeWindow = deliveryTW;
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
    public final long getPickupDuration() {
        return pickupDuration;
    }

    /**
     * @return {@link #deliveryDuration}
     */
    public final long getDeliveryDuration() {
        return deliveryDuration;
    }

    /**
     * @return {@link #destination}
     */
    public final Point getDestination() {
        return destination;
    }

    public final TimeWindow getDeliveryTimeWindow() {
        return deliveryTimeWindow;
    }

    public final TimeWindow getPickupTimeWindow() {
        return pickupTimeWindow;
    }

    // can be overriden to add problem specific constraints on pickup
    public boolean canBePickedUp(Vehicle v, long time) {
        return true;
    }

    public boolean canBeDelivered(Vehicle v, long time) {
        return true;
    }

}
