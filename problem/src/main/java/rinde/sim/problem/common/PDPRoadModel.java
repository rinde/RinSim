package rinde.sim.problem.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newHashMap;

import java.util.Map;
import java.util.Queue;

import javax.annotation.Nullable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.core.model.ModelReceiver;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPModel.ParcelState;
import rinde.sim.core.model.road.AbstractRoadModel;
import rinde.sim.core.model.road.ForwardingRoadModel;
import rinde.sim.core.model.road.MoveProgress;
import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.road.RoadUser;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * A decorator for {@link RoadModel} which provides a more convenient API for
 * PDP problems. TODO explain what!
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class PDPRoadModel extends ForwardingRoadModel implements ModelReceiver {

    protected final Map<MovingRoadUser, DestinationObject> destinations;
    protected final Multimap<MovingRoadUser, DestinationObject> destinationHistory;
    protected final AbstractRoadModel<?> delegate;
    protected final boolean allowDiversion;

    protected PDPModel pdpModel;

    /**
     * Decorates the {@link RoadModel}
     * @param rm
     * @param allowVehicleDiversion
     */
    public PDPRoadModel(AbstractRoadModel<?> rm, boolean allowVehicleDiversion) {
        allowDiversion = allowVehicleDiversion;
        delegate = rm;
        destinations = newHashMap();
        // does not allow duplicates: WE NEED THIS
        destinationHistory = LinkedHashMultimap.create();
    }

    public boolean isVehicleDiversionAllowed() {
        return allowDiversion;
    }

    @Override
    protected AbstractRoadModel<?> delegate() {
        return delegate;
    }

    private void checkType(RoadUser ru) {
        checkArgument(
            ru instanceof DefaultVehicle || ru instanceof DefaultDepot
                    || ru instanceof DefaultParcel,
            "This RoadModel only allows instances of DefaultVehicle, DefaultDepot and DefaultParcel.");
    }

    @Override
    public boolean equalPosition(RoadUser obj1, RoadUser obj2) {
        return getParcelPos(obj1).equals(getParcelPos(obj2));
    }

    Point getParcelPos(RoadUser obj) {
        if (!containsObject(obj) && obj instanceof DefaultParcel) {
            final ParcelState state =
                    pdpModel.getParcelState((DefaultParcel) obj);
            checkArgument(state == ParcelState.IN_CARGO,
                "Can only move to parcels which are either on the map or in cargo.");
            return ((DefaultParcel) obj).getDestination();
        }
        return getPosition(obj);
    }

    @Override
    public void addObjectAt(RoadUser newObj, Point pos) {
        checkType(newObj);
        delegate.addObjectAt(newObj, pos);
    }

    @Override
    public void addObjectAtSamePosition(RoadUser newObj, RoadUser existingObj) {
        checkType(newObj);
        delegate.addObjectAtSamePosition(newObj, existingObj);
    }

    @Override
    public MoveProgress moveTo(MovingRoadUser object,
            RoadUser destinationObject, TimeLapse time) {

        DestinationObject newDestinationObject;
        if (destinationObject instanceof DefaultParcel) {
            final DefaultParcel dp = (DefaultParcel) destinationObject;
            final DestType type =
                    containsObject(dp) ? DestType.PICKUP : DestType.DELIVERY;
            final Point pos = getParcelPos(destinationObject);
            if (type == DestType.DELIVERY) {
                checkArgument(
                    pdpModel.containerContains((DefaultVehicle) object,
                        (DefaultParcel) destinationObject),
                    "A vehicle can only move to the delivery location of a parcel if it is carrying it.");
            }
            // type == DestType.PICKUP ? dp.dto.pickupLocation
            // : dp.dto.destinationLocation;
            newDestinationObject = new DestinationObject(type, pos, dp);
        } else {
            newDestinationObject =
                    new DestinationObject(DestType.DEPOT,
                            getPosition(destinationObject), destinationObject);
        }

        boolean destChange = true;
        if (destinations.containsKey(object) && !allowDiversion) {
            final DestinationObject prev = destinations.get(object);
            final boolean atDestination = getPosition(object).equals(prev.dest);

            if (!atDestination && prev.type != DestType.DEPOT) {
                // when we haven't reached our destination and the destination
                // isn't the depot we are not allowed to change destination

                boolean alreadyServiced = false;
                if (prev.type == DestType.PICKUP) {
                    alreadyServiced =
                            pdpModel.getParcelState((DefaultParcel) prev.roadUser) == ParcelState.PICKING_UP
                                    || pdpModel
                                            .getParcelState((DefaultParcel) prev.roadUser) == ParcelState.IN_CARGO;
                } else if (prev.type == DestType.DELIVERY) {
                    alreadyServiced =
                            pdpModel.getParcelState((DefaultParcel) prev.roadUser) == ParcelState.DELIVERING
                                    || pdpModel
                                            .getParcelState((DefaultParcel) prev.roadUser) == ParcelState.DELIVERED;
                }

                checkArgument(
                    prev.roadUser == destinationObject || alreadyServiced,
                    "Diversion from the current destination is not allowed: %s.",
                    prev.dest);
                destChange = false;
            } else {
                // change destination
                if (prev.type == DestType.PICKUP) {
                    // when we are at the prev destination, and it was a pickup,
                    // we are allowed to move if it has been picked up
                    checkArgument(
                        pdpModel.getParcelState((DefaultParcel) prev.roadUser) != ParcelState.AVAILABLE,
                        "Can not move away before the parcel has been picked up: %s.",
                        prev.roadUser);
                } else if (prev.type == DestType.DELIVERY) {
                    // when we are at the prev destination and it was a
                    // delivery, we are allowed to move to other objects only,
                    // and only if the parcel is delivered
                    checkArgument(
                        prev.roadUser != destinationObject,
                        "Can not move to the same parcel since we are already there: %s.",
                        prev.roadUser);
                    checkArgument(
                        pdpModel.getParcelState((DefaultParcel) prev.roadUser) == ParcelState.DELIVERED,
                        "The parcel needs to be delivered before moving away: %s.",
                        prev.roadUser);
                } else {// it is a depot
                    // the destination is only changed in case we are no longer
                    // going towards the depot
                    destChange = newDestinationObject.type != DestType.DEPOT;
                }
            }
        }
        // if (destChange && !allowDiversion) {
        // checkArgument(
        // !destinations.inverse().containsKey(newDestinationObject),
        // "Only one vehicle is allowed to travel towards a Parcel.");
        // }
        destinations.put(object, newDestinationObject);
        if (destChange) {

            checkArgument(
                allowDiversion
                        || !destinationHistory.containsEntry(object,
                            newDestinationObject),
                "It is not allowed to revisit this parcel, it should have been picked up and been delivered already: %s.",
                newDestinationObject.roadUser);
            destinationHistory.put(object, newDestinationObject);
        }
        return delegate.moveTo(object, newDestinationObject.dest, time);
    }

    /**
     * Returns the destination of the specified {@link MovingRoadUser} if
     * <i>all</i> of the following holds:
     * <ul>
     * <li>The {@link MovingRoadUser} is moving to some destination.</li>
     * <li>The destination is a {@link DefaultParcel}.</li>
     * <li>The {@link MovingRoadUser} has not yet reached its destination.</li>
     * </ul>
     * Returns <code>null</code> otherwise.
     * @param obj The {@link MovingRoadUser} to check its destination for.
     * @return The parcel the road user is heading to or <code>null</code>
     *         otherwise.
     */
    @Nullable
    public DefaultParcel getDestinationToParcel(MovingRoadUser obj) {
        if (destinations.containsKey(obj)
                && destinations.get(obj).type != DestType.DEPOT
                && !destinations.get(obj).dest.equals(getPosition(obj))) {
            return (DefaultParcel) destinations.get(obj).roadUser;
        }
        return null;
    }

    @Override
    public MoveProgress moveTo(MovingRoadUser object, Point destination,
            TimeLapse time) {
        throw new UnsupportedOperationException(
                "This road model only supports the moveTo(MovingRoadUser,RoadUser,TimeLapse) method.");
    }

    @Override
    public final MoveProgress followPath(MovingRoadUser object,
            Queue<Point> path, TimeLapse time) {
        throw new UnsupportedOperationException(
                "This road model only supports the moveTo(MovingRoadUser,RoadUser,TimeLapse) method.");
    }

    private static final class DestinationObject {
        public final DestType type;
        public final Point dest;
        public final RoadUser roadUser;
        private final int hashCode;

        DestinationObject(DestType type, Point dest, RoadUser obj) {
            this.type = type;
            this.dest = dest;
            roadUser = obj;
            hashCode =
                    new HashCodeBuilder(17, 31).append(type).append(dest)
                            .append(obj).toHashCode();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        // since this class is only used internally, we don't have to check for
        // null or instanceof
        @SuppressWarnings("null")
        @Override
        public boolean equals(@Nullable Object obj) {
            final DestinationObject other = (DestinationObject) obj;
            return new EqualsBuilder()//
                    .append(type, other.type)//
                    .append(dest, other.dest)//
                    .append(roadUser, other.roadUser).isEquals();
        }
    }

    enum DestType {
        PICKUP, DELIVERY, DEPOT;
    }

    @Override
    public void registerModelProvider(ModelProvider mp) {
        pdpModel = mp.getModel(PDPModel.class);

    }
}
