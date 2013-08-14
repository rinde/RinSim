package rinde.sim.problem.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newLinkedHashMap;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import javax.annotation.Nullable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.MoveEvent;
import rinde.sim.core.model.road.MoveProgress;
import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.core.model.road.RoadUser;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * A {@link PlaneRoadModel} which does not allow diversion of vehicles.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class NoDiversionPlaneRoadModel extends PlaneRoadModel implements
        NoDiversionRoadModel {

    final Map<MovingRoadUser, DestinationObject> destinations;
    final Multimap<MovingRoadUser, DestinationObject> destinationHistory;

    /**
     * Create a new plane road model using the specified boundaries and max
     * speed.
     * @param pMin The minimum x and y of the plane.
     * @param pMax The maximum x and y of the plane.
     * @param useSpeedConversion Use speed conversion.
     * @param pMaxSpeed The maximum speed that objects can travel on the plane.
     */
    public NoDiversionPlaneRoadModel(Point pMin, Point pMax,
            boolean useSpeedConversion, double pMaxSpeed) {
        super(pMin, pMax, useSpeedConversion, pMaxSpeed);
        destinations = newLinkedHashMap();
        // does not allow duplicates: WE NEED THIS
        destinationHistory = LinkedHashMultimap.create();
    }

    private void checkType(RoadUser ru) {
        checkArgument(ru instanceof DefaultVehicle
                || ru instanceof DefaultDepot || ru instanceof DefaultParcel, "This RoadModel only allows instances of DefaultVehicle, DefaultDepot and DefaultParcel.");
    }

    @Override
    public void addObjectAt(RoadUser newObj, Point pos) {
        checkType(newObj);
        super.addObjectAt(newObj, pos);
    }

    @Override
    public void addObjectAtSamePosition(RoadUser newObj, RoadUser existingObj) {
        checkType(newObj);
        super.addObjectAtSamePosition(newObj, existingObj);
    }

    @Override
    public MoveProgress moveTo(MovingRoadUser object,
            RoadUser destinationObject, TimeLapse time) {

        DestinationObject newDestinationObject;
        if (destinationObject instanceof DefaultParcel) {
            final DefaultParcel dp = (DefaultParcel) destinationObject;
            final DestType type = containsObject(dp) ? DestType.PICKUP
                    : DestType.DELIVERY;
            final Point pos = type == DestType.PICKUP ? dp.dto.pickupLocation
                    : dp.dto.destinationLocation;
            newDestinationObject = new DestinationObject(type, pos, dp);
        } else {
            newDestinationObject = new DestinationObject(DestType.DEPOT,
                    getPosition(destinationObject), destinationObject);
        }

        boolean destChange = true;
        if (destinations.containsKey(object)) {
            final DestinationObject prev = destinations.get(object);
            final boolean atDestination = getPosition(object).equals(prev.dest);
            final boolean isOnMap = containsObject(prev.roadUser);

            if (!atDestination && prev.type != DestType.DEPOT) {
                // when we haven't reached our destination and the destination
                // isn't the depot we are not allowed to change destination
                checkArgument(prev.roadUser == destinationObject, "Diversion from the current destination is not allowed: %s.", prev.dest);
                destChange = false;
            } else {
                // change destination
                if (prev.type == DestType.PICKUP) {
                    // when we are at the prev destination, and it was a pickup,
                    // we are allowed to move if it has been picked up
                    checkArgument(!isOnMap, "Can not move away before the parcel has been picked up: %s.", prev.roadUser);
                } else if (prev.type == DestType.DELIVERY) {
                    // when we are at the prev destination and it was a
                    // delivery, we are allowed to move to other objects only
                    checkArgument(prev.roadUser != destinationObject, "Can not move to the same parcel again, it has already been picked up: %s.", prev.roadUser);
                } else {// it is a depot
                    // the destination is only changed in case we are no longer
                    // going towards the depot
                    destChange = newDestinationObject.type != DestType.DEPOT;
                }
            }
        }
        destinations.put(object, newDestinationObject);
        if (destChange) {
            checkArgument(!destinationHistory.containsEntry(object, newDestinationObject), "It is not allowed to revisit this parcel, it should have been picked up and been delivered already: %s.", newDestinationObject.roadUser);
            destinationHistory.put(object, newDestinationObject);
        }
        return move(object, newDestinationObject.dest, time);
    }

    private MoveProgress move(MovingRoadUser object, Point dest, TimeLapse time) {
        // actual moving
        Queue<Point> path;
        if (objDestinations.containsKey(object)
                && objDestinations.get(object).destination.equals(dest)) {
            // is valid move? -> assume it is
            path = objDestinations.get(object).path;
        } else {
            path = new LinkedList<Point>(getShortestPathTo(object, dest));
            objDestinations.put(object, new DestinationPath(dest, path));
        }
        final MoveProgress mp = super.doFollowPath(object, path, time);
        eventDispatcher.dispatchEvent(new MoveEvent(this, object, mp));
        return mp;
    }

    @Override
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
    protected final MoveProgress doFollowPath(MovingRoadUser object,
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
            hashCode = new HashCodeBuilder(17, 31).append(type).append(dest)
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
}
