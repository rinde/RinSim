package com.github.rinde.rinsim.pdptw.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newHashMap;

import java.util.Map;
import java.util.Queue;

import javax.annotation.Nullable;

import org.apache.commons.lang3.builder.EqualsBuilder;

import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.graph.Point;
import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.ModelReceiver;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.road.AbstractRoadModel;
import com.github.rinde.rinsim.core.model.road.ForwardingRoadModel;
import com.github.rinde.rinsim.core.model.road.MoveProgress;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * A decorator for {@link AbstractRoadModel} which provides a more convenient
 * API for PDP problems and it can control whether vehicles are allowed to
 * divert.
 * <p>
 * <b>Pickup and delivery problem convenience API</b><br/>
 * This model allows the following: {@code
 *  moveTo(v1,p1,..)
 *  pickup(v1,p1,..)
 *  moveTo(v1,p1,..)
 *  deliver(v1,p1,..)
 * } This means that you only need the reference to the parcel which you are
 * interested in, depending on its state the vehicle will automatically move to
 * the pickup location or the delivery location.
 * <p>
 * <b>Diversion</b><br/>
 * This model can optionally disallow vehicle diversion. Vehicle diversion is
 * defined as a vehicle changing its destination service location before it has
 * completed servicing that service location. More concretely if a vehicle
 * <code>v1</code> is moving to the pickup location of parcel <code>p1</code>
 * but then changes its destination to the pickup location of parcel
 * <code>p2</code> we say that vehicle <code>v1</code> has diverted.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class PDPRoadModel extends ForwardingRoadModel implements ModelReceiver {

  final Map<MovingRoadUser, DestinationObject> destinations;
  final Multimap<MovingRoadUser, DestinationObject> destinationHistory;
  final boolean allowDiversion;
  Optional<PDPModel> pdpModel;

  /**
   * Decorates the {@link AbstractRoadModel}.
   * @param rm The road model that is being decorated by this model.
   * @param allowVehicleDiversion Should the model allow vehicle diversion or
   *          not. See {@link PDPRoadModel} for more information about
   *          diversion.
   */
  public PDPRoadModel(AbstractRoadModel<?> rm, boolean allowVehicleDiversion) {
    super(rm);
    allowDiversion = allowVehicleDiversion;
    destinations = newHashMap();
    // does not allow duplicates: WE NEED THIS
    destinationHistory = LinkedHashMultimap.create();
    pdpModel = Optional.absent();
  }

  /**
   * @return <code>true</code> when diversion is allowed, <code>false</code>
   *         otherwise. See {@link PDPRoadModel} for more information about
   *         diversion.
   */
  public boolean isVehicleDiversionAllowed() {
    return allowDiversion;
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
      final ParcelState state = pdpModel.get().getParcelState(
          (DefaultParcel) obj);
      checkArgument(
          state == ParcelState.IN_CARGO,
          "Can only move to parcels which are either on the map or in cargo, state is %s, obj is %s.",
          state, obj);
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

  private boolean isAlreadyServiced(DestType type, RoadUser ru) {
    final PDPModel pm = pdpModel.get();
    boolean alreadyServiced = false;
    if (type == DestType.PICKUP) {
      alreadyServiced = pm.getParcelState((DefaultParcel) ru) == ParcelState.PICKING_UP
          || pm.getParcelState((DefaultParcel) ru) == ParcelState.IN_CARGO;
    } else if (type == DestType.DELIVERY) {
      alreadyServiced = pm.getParcelState((DefaultParcel) ru) == ParcelState.DELIVERING
          || pm.getParcelState((DefaultParcel) ru) == ParcelState.DELIVERED;
    }
    return alreadyServiced;
  }

  @Override
  public MoveProgress moveTo(MovingRoadUser object,
      RoadUser destinationRoadUser, TimeLapse time) {
    final PDPModel pm = pdpModel.get();
    DestinationObject newDestinationObject;
    if (destinationRoadUser instanceof DefaultParcel) {
      final DefaultParcel dp = (DefaultParcel) destinationRoadUser;
      final DestType type = containsObject(dp) ? DestType.PICKUP
          : DestType.DELIVERY;
      final Point pos = getParcelPos(destinationRoadUser);
      if (type == DestType.DELIVERY) {
        checkArgument(
            pm.containerContains((DefaultVehicle) object,
                (DefaultParcel) destinationRoadUser),
            "A vehicle can only move to the delivery location of a parcel if it is carrying it.");
      }
      newDestinationObject = new DestinationObject(type, pos, dp);
    } else {
      newDestinationObject = new DestinationObject(DestType.DEPOT,
          getPosition(destinationRoadUser), destinationRoadUser);
    }

    boolean destChange = true;
    if (destinations.containsKey(object) && !allowDiversion) {
      final DestinationObject prev = destinations.get(object);
      final boolean atDestination = getPosition(object).equals(prev.dest);

      if (!atDestination && prev.type != DestType.DEPOT) {
        // when we haven't reached our destination and the destination
        // isn't the depot we are not allowed to change destination
        checkArgument(
            prev.roadUser == destinationRoadUser
                || isAlreadyServiced(prev.type, prev.roadUser),
            "Diversion from the current destination is not allowed. Prev: %s, new: %s.",
            prev, destinationRoadUser);
        destChange = false;
      } else
      // change destination
      if (prev.type == DestType.PICKUP) {
        // when we are at the prev destination, and it was a pickup, we are
        // allowed to move if it has been picked up
        checkArgument(
            pm.getParcelState((DefaultParcel) prev.roadUser) != ParcelState.AVAILABLE,
            "Can not move away before the parcel has been picked up: %s.",
            prev.roadUser);
      } else if (prev.type == DestType.DELIVERY) {
        // when we are at the prev destination and it was a delivery, we are
        // allowed to move to other objects only, and only if the parcel is
        // delivered
        checkArgument(prev.roadUser != destinationRoadUser,
            "Can not move to the same parcel since we are already there: %s.",
            prev.roadUser);
        checkArgument(
            pm.getParcelState((DefaultParcel) prev.roadUser) == ParcelState.DELIVERED,
            "The parcel needs to be delivered before moving away: %s.",
            prev.roadUser);
      } else {
        // it is a depot
        // the destination is only changed in case we are no longer going
        // towards the depot
        destChange = newDestinationObject.type != DestType.DEPOT;
      }
    }

    destinations.put(object, newDestinationObject);
    if (destChange && newDestinationObject.type != DestType.DEPOT) {
      checkArgument(
          allowDiversion
              || !destinationHistory
                  .containsEntry(object, newDestinationObject),
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
   * <li>The {@link MovingRoadUser} has not yet started servicing at its
   * destination.</li>
   * </ul>
   * Returns <code>null</code> otherwise.
   * @param obj The {@link MovingRoadUser} to check its destination for.
   * @return The parcel the road user is heading to or <code>null</code>
   *         otherwise.
   */
  @Nullable
  public DefaultParcel getDestinationToParcel(MovingRoadUser obj) {
    if (destinations.containsKey(obj)
        && destinations.get(obj).type != DestType.DEPOT) {
      final ParcelState parcelState = pdpModel.get().getParcelState(
          (Parcel) destinations.get(obj).roadUser);
      // if it is a pickup destination it must still be available
      if (destinations.get(obj).type == DestType.PICKUP
          && parcelState == ParcelState.AVAILABLE
          || parcelState == ParcelState.ANNOUNCED
          // if it is a delivery destination it must still be in cargo
          || destinations.get(obj).type == DestType.DELIVERY
          && parcelState == ParcelState.IN_CARGO) {
        return (DefaultParcel) destinations.get(obj).roadUser;
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * @throws UnsupportedOperationException when diversion is not allowed.
   */
  @Override
  public MoveProgress moveTo(MovingRoadUser object, Point destination,
      TimeLapse time) {
    if (allowDiversion) {
      return delegate.moveTo(object, destination, time);
    } else {
      return unsupported();
    }
  }

  /**
   * {@inheritDoc}
   * @throws UnsupportedOperationException when diversion is not allowed.
   */
  @Override
  public final MoveProgress followPath(MovingRoadUser object,
      Queue<Point> path, TimeLapse time) {
    if (allowDiversion) {
      return delegate.followPath(object, path, time);
    } else {
      return unsupported();
    }
  }

  private MoveProgress unsupported() {
    throw new UnsupportedOperationException(
        "This road model doesn't allow diversion and therefore only supports the moveTo(MovingRoadUser,RoadUser,TimeLapse) method.");
  }

  @Override
  public void registerModelProvider(ModelProvider mp) {
    pdpModel = Optional.fromNullable(mp.getModel(PDPModel.class));
  }

  private static final class DestinationObject {
    final DestType type;
    final Point dest;
    final RoadUser roadUser;
    private final int hashCode;

    DestinationObject(DestType type, Point dest, RoadUser obj) {
      this.type = type;
      this.dest = dest;
      roadUser = obj;
      hashCode = Objects.hashCode(type, dest, obj);
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj == null) {
        return false;
      }
      if (obj == this) {
        return true;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final DestinationObject other = (DestinationObject) obj;
      return new EqualsBuilder().append(type, other.type)
          .append(dest, other.dest).append(roadUser, other.roadUser).isEquals();
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this).add("type", type).add("dest", dest)
          .add("roadUser", roadUser).toString();
    }
  }

  enum DestType {
    PICKUP, DELIVERY, DEPOT;
  }
}
