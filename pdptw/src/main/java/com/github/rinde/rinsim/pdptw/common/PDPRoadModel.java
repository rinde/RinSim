/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.pdptw.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newHashMap;

import java.util.Map;
import java.util.Queue;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.ModelReceiver;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.AbstractRoadModel;
import com.github.rinde.rinsim.core.model.road.ForwardingRoadModel;
import com.github.rinde.rinsim.core.model.road.GenericRoadModel;
import com.github.rinde.rinsim.core.model.road.MoveProgress;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.GeomHeuristic;
import com.github.rinde.rinsim.geom.GeomHeuristics;
import com.github.rinde.rinsim.geom.Point;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * A decorator for {@link AbstractRoadModel} which provides a more convenient
 * API for PDP problems and it can control whether vehicles are allowed to
 * divert.
 * <p>
 * <b>Pickup and delivery problem convenience API</b><br>
 * This model allows the following: {@code
 *  moveTo(v1,p1,..)
 *  pickup(v1,p1,..)
 *  moveTo(v1,p1,..)
 *  deliver(v1,p1,..)
 * } This means that you only need the reference to the parcel which you are
 * interested in, depending on its state the vehicle will automatically move to
 * the pickup location or the delivery location.
 * <p>
 * <b>Diversion</b><br>
 * This model can optionally disallow vehicle diversion. Vehicle diversion is
 * defined as a vehicle changing its destination service location before it has
 * completed servicing that service location. More concretely if a vehicle
 * <code>v1</code> is moving to the pickup location of parcel <code>p1</code>
 * but then changes its destination to the pickup location of parcel
 * <code>p2</code> we say that vehicle <code>v1</code> has diverted.
 *
 * @author Rinde van Lon
 */
public class PDPRoadModel extends ForwardingRoadModel<GenericRoadModel>
    implements ModelReceiver {

  final Map<MovingRoadUser, DestinationObject> destinations;
  final Multimap<MovingRoadUser, DestinationObject> destinationHistory;
  final boolean allowDiversion;
  Optional<PDPModel> pdpModel;

  PDPRoadModel(AbstractRoadModel rm, boolean diversion) {
    super(rm);
    allowDiversion = diversion;
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
    checkArgument(ru instanceof Vehicle
      || ru instanceof Depot
      || ru instanceof Parcel,
      "This RoadModel only allows instances of Vehicle, Depot and Parcel.");
  }

  @Override
  public boolean equalPosition(RoadUser obj1, RoadUser obj2) {
    return getParcelPos(obj1).equals(getParcelPos(obj2));
  }

  Point getParcelPos(RoadUser obj) {
    if (!containsObject(obj) && obj instanceof Parcel) {
      final ParcelState state = pdpModel.get().getParcelState(
        (Parcel) obj);
      checkArgument(
        state == ParcelState.IN_CARGO,
        "Can only move to parcels which are either on the map or in cargo, "
          + "state is %s, obj is %s.",
        state, obj);
      return ((Parcel) obj).getDeliveryLocation();
    }
    return getPosition(obj);
  }

  @Override
  public void addObjectAt(RoadUser newObj, Point pos) {
    checkType(newObj);
    delegate().addObjectAt(newObj, pos);
  }

  @Override
  public void addObjectAtSamePosition(RoadUser newObj, RoadUser existingObj) {
    checkType(newObj);
    delegate().addObjectAtSamePosition(newObj, existingObj);
  }

  private boolean isAlreadyServiced(DestType type, RoadUser ru) {
    boolean alreadyServiced = false;
    final Parcel p = (Parcel) ru;
    if (type == DestType.PICKUP) {
      alreadyServiced =
        pdpModel.get().getParcelState(p) == ParcelState.PICKING_UP
          || pdpModel.get().getParcelState(p) == ParcelState.IN_CARGO;
    } else if (type == DestType.DELIVERY) {
      alreadyServiced =
        pdpModel.get().getParcelState(p) == ParcelState.DELIVERING
          || pdpModel.get().getParcelState(p) == ParcelState.DELIVERED;
    }
    return alreadyServiced;
  }

  @Override
  public MoveProgress moveTo(MovingRoadUser object,
      RoadUser destinationRoadUser, TimeLapse time) {
    return moveTo(object, destinationRoadUser, time,
      GeomHeuristics.euclidean());
  }

  @Override
  public MoveProgress moveTo(MovingRoadUser object,
      RoadUser destinationRoadUser, TimeLapse time, GeomHeuristic heuristic) {
    final DestinationObject newDestinationObject;
    if (destinationRoadUser instanceof Parcel) {
      final Parcel dp = (Parcel) destinationRoadUser;
      final DestType type = containsObject(dp) ? DestType.PICKUP
        : DestType.DELIVERY;
      final Point pos = getParcelPos(destinationRoadUser);
      if (type == DestType.DELIVERY) {
        checkArgument(
          pdpModel.get().containerContains((Vehicle) object,
            (Parcel) destinationRoadUser),
          "A vehicle can only move to the delivery location of a parcel if it"
            + " is carrying it.");
      }
      newDestinationObject = DestinationObject.create(type, pos, dp);
    } else {
      newDestinationObject = DestinationObject.create(DestType.DEPOT,
        getPosition(destinationRoadUser), destinationRoadUser);
    }

    boolean destChange = true;
    if (destinations.containsKey(object) && !allowDiversion) {
      final DestinationObject prev = destinations.get(object);
      final boolean atDestination = getPosition(object).equals(prev.dest());

      if (!atDestination && prev.type() != DestType.DEPOT) {
        // when we haven't reached our destination and the destination
        // isn't the depot we are not allowed to change destination
        checkArgument(
          prev.roadUser() == destinationRoadUser
            || isAlreadyServiced(prev.type(), prev.roadUser()),
          "Diversion from the current destination is not allowed. "
            + "Prev: %s, new: %s.",
          prev, destinationRoadUser);
        destChange = false;
      } else
      // change destination
      if (prev.type() == DestType.PICKUP) {
        // when we are at the prev destination, and it was a pickup, we are
        // allowed to move if it has been picked up
        checkArgument(
          pdpModel.get().getParcelState(
            (Parcel) prev.roadUser()) != ParcelState.AVAILABLE,
          "Can not move away before the parcel has been picked up: %s.",
          prev.roadUser());
      } else if (prev.type() == DestType.DELIVERY) {
        // when we are at the prev destination and it was a delivery, we are
        // allowed to move to other objects only, and only if the parcel is
        // delivered
        checkArgument(prev.roadUser() != destinationRoadUser,
          "Can not move to the same parcel since we are already there: %s.",
          prev.roadUser());
        checkArgument(
          pdpModel.get().getParcelState(
            (Parcel) prev.roadUser()) == ParcelState.DELIVERED,
          "The parcel needs to be delivered before moving away: %s.",
          prev.roadUser());
      } else {
        // it is a depot
        // the destination is only changed in case we are no longer going
        // towards the depot
        destChange = newDestinationObject.type() != DestType.DEPOT;
      }
    }

    destinations.put(object, newDestinationObject);
    if (destChange && newDestinationObject.type() != DestType.DEPOT) {
      checkArgument(
        allowDiversion
          || !destinationHistory
            .containsEntry(object, newDestinationObject),
        "It is not allowed to revisit this parcel, it should have been picked"
          + " up and been delivered already: %s.",
        newDestinationObject.roadUser());
      destinationHistory.put(object, newDestinationObject);
    }
    return delegate().moveTo(object, newDestinationObject.dest(), time,
      heuristic);
  }

  /**
   * Returns the destination of the specified {@link MovingRoadUser} if
   * <i>all</i> of the following holds:
   * <ul>
   * <li>The {@link MovingRoadUser} is moving to some destination.</li>
   * <li>The destination is a {@link Parcel}.</li>
   * <li>The {@link MovingRoadUser} has not yet started servicing at its
   * destination.</li>
   * </ul>
   * Returns <code>null</code> otherwise.
   * @param obj The {@link MovingRoadUser} to check its destination for.
   * @return The parcel the road user is heading to or <code>null</code>
   *         otherwise.
   */
  @Nullable
  public Parcel getDestinationToParcel(MovingRoadUser obj) {
    if (destinations.containsKey(obj)
      && destinations.get(obj).type() != DestType.DEPOT) {
      final ParcelState parcelState = pdpModel.get().getParcelState(
        (Parcel) destinations.get(obj).roadUser());
      // if it is a pickup destination it must still be available
      if (destinations.get(obj).type() == DestType.PICKUP
        && parcelState == ParcelState.AVAILABLE
        || parcelState == ParcelState.ANNOUNCED
      // if it is a delivery destination it must still be in cargo
        || destinations.get(obj).type() == DestType.DELIVERY
          && parcelState == ParcelState.IN_CARGO) {
        return (Parcel) destinations.get(obj).roadUser();
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
      return delegate().moveTo(object, destination, time);
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
      return delegate().followPath(object, path, time);
    } else {
      return unsupported();
    }
  }

  private MoveProgress unsupported() {
    throw new UnsupportedOperationException(
      "This road model doesn't allow diversion and therefore only supports "
        + "the moveTo(MovingRoadUser,RoadUser,TimeLapse) method.");
  }

  @Override
  public void registerModelProvider(ModelProvider mp) {
    pdpModel = Optional.of(mp.getModel(PDPModel.class));
  }

  @Override
  public <U> U get(Class<U> type) {
    return type.cast(self);
  }

  /**
   * Create a new {@link Builder} instance.
   * @param delegate The {@link ModelBuilder} that constructs the
   *          {@link RoadModel} that is going to be decorated.
   * @return A new {@link Builder} that constructs {@link PDPRoadModel}
   *         instances that decorate the <code>delegate</code>.
   */
  public static Builder builder(
      ModelBuilder<? extends RoadModel, ? extends RoadUser> delegate) {
    return Builder.create(delegate, false);
  }

  /**
   * Builder for {@link PDPRoadModel}.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder
      extends AbstractBuilder<PDPRoadModel, RoadModel> {

    private static final long serialVersionUID = -2254105659128952997L;

    Builder() {
      setProvidingTypes(RoadModel.class, PDPRoadModel.class);
    }

    @Override
    public abstract ModelBuilder<RoadModel, RoadUser> getDelegateModelBuilder();

    @Override
    public PDPRoadModel build(DependencyProvider dependencyProvider) {
      return new PDPRoadModel(
        (AbstractRoadModel) getDelegateModelBuilder()
          .build(dependencyProvider),
        getAllowVehicleDiversion());
    }

    @Override
    public Builder withAllowVehicleDiversion(boolean allowDiversion) {
      return create(getDelegateModelBuilder(), allowDiversion);
    }

    @Override
    public String toString() {
      return Joiner.on("").join(
        PDPRoadModel.class.getSimpleName(),
        ".builder(", getDelegateModelBuilder(), ")");
    }

    @SuppressWarnings("unchecked")
    static Builder create(
        ModelBuilder<? extends RoadModel, ? extends RoadUser> delegateModelBuilder,
        boolean allowDiversion) {
      return new AutoValue_PDPRoadModel_Builder(allowDiversion,
        (ModelBuilder<RoadModel, RoadUser>) delegateModelBuilder);
    }

  }

  /**
   * Builder for constructing {@link PDPRoadModel} instances. Instances can be
   * obtained via {@link PDPRoadModel#builder(ModelBuilder)}.
   * @author Rinde van Lon
   */
  abstract static class AbstractBuilder<T extends PDPRoadModel, U extends RoadModel>
      extends ForwardingRoadModel.Builder<T> {

    private static final long serialVersionUID = 5571499433551975529L;

    AbstractBuilder() {}

    @Override
    public abstract ModelBuilder<U, RoadUser> getDelegateModelBuilder();

    /**
     * @return <code>true</code> if vehicle diversion is allowed,
     *         <code>false</code> otherwise.
     */
    public abstract boolean getAllowVehicleDiversion();

    @CheckReturnValue
    public abstract AbstractBuilder<T, U> withAllowVehicleDiversion(
        boolean allowDiversion);

  }

  @AutoValue
  abstract static class DestinationObject {

    DestinationObject() {}

    static DestinationObject create(DestType type, Point dest, RoadUser obj) {
      return new AutoValue_PDPRoadModel_DestinationObject(type, dest, obj);
    }

    abstract DestType type();

    abstract Point dest();

    abstract RoadUser roadUser();
  }

  enum DestType {
    PICKUP, DELIVERY, DEPOT;
  }
}
