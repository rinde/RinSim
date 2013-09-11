package rinde.sim.pdptw.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newHashMap;

import java.util.Map;
import java.util.Queue;

import javax.annotation.Nullable;

import org.apache.commons.lang3.builder.EqualsBuilder;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.core.model.ModelReceiver;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPModel.ParcelState;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.road.AbstractRoadModel;
import rinde.sim.core.model.road.ForwardingRoadModel;
import rinde.sim.core.model.road.MoveProgress;
import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.RoadUser;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * A decorator for {@link AbstractRoadModel} which provides a more convenient
 * API for PDP problems. TODO explain what!
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class PDPRoadModel extends ForwardingRoadModel implements ModelReceiver {

  protected final Map<MovingRoadUser, DestinationObject> destinations;
  protected final Multimap<MovingRoadUser, DestinationObject> destinationHistory;
  protected final AbstractRoadModel<?> delegate;
  protected final boolean allowDiversion;

  protected Optional<PDPModel> pdpModel;

  /**
   * Decorates the {@link AbstractRoadModel}
   * @param rm
   * @param allowVehicleDiversion
   */
  public PDPRoadModel(AbstractRoadModel<?> rm, boolean allowVehicleDiversion) {
    allowDiversion = allowVehicleDiversion;
    delegate = rm;
    destinations = newHashMap();
    // does not allow duplicates: WE NEED THIS
    destinationHistory = LinkedHashMultimap.create();
    pdpModel = Optional.absent();
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
      final ParcelState state = pdpModel.get().getParcelState(
          (DefaultParcel) obj);
      checkArgument(
          state == ParcelState.IN_CARGO,
          "Can only move to parcels which are either on the map or in cargo, state is %s.",
          state, obj.hashCode());
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
      if ((destinations.get(obj).type == DestType.PICKUP
          && parcelState == ParcelState.AVAILABLE || parcelState == ParcelState.ANNOUNCED)
          // if it is a delivery destination it must still be in cargo
          || (destinations.get(obj).type == DestType.DELIVERY && parcelState == ParcelState.IN_CARGO)) {
        return (DefaultParcel) destinations.get(obj).roadUser;
      }
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
      hashCode = Objects.hashCode(type, dest, obj);
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj == null || getClass() != this.getClass()) {
        return false;
      }
      if (obj == this) {
        return true;
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

  @Override
  public void registerModelProvider(ModelProvider mp) {
    pdpModel = Optional.fromNullable(mp.getModel(PDPModel.class));
  }
}
