/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.core.model.pdp;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.EventAPI;
import com.google.common.collect.ImmutableSet;

/**
 * Defines the public interface for a model for pickup-and-delivery problems.
 * This model is only responsible for the picking up and delivery operations,
 * i.e. it is not responsible for movement.
 * @author Rinde van Lon
 */
public abstract class PDPModel extends AbstractModel<PDPObject> implements
  TickListener {

  /**
   * The logger of the model.
   */
  protected static final Logger LOGGER = LoggerFactory
    .getLogger(PDPModel.class);

  /**
   * Reference to the outermost decorator of this {@link PDPModel} instance.
   */
  protected PDPModel self = this;
  private boolean initialized = false;

  /**
   * Method which should by called by a decorator of this instance.
   * @param pm The decorator.
   */
  protected void setSelf(PDPModel pm) {
    LOGGER.info("setSelf {}", pm);
    checkState(!initialized,
      "This PDPModel is already initialized, it is too late to decorate it.");
    self = pm;
  }

  @Override
  public final boolean register(PDPObject object) {
    initialized = true;
    return doRegister(object);
  }

  /**
   * This method should be called by {@link Vehicle}s that need to finish a
   * previously started operation. By calling this method before executing other
   * actions, time consistency is enforced since any pending actions will
   * consume time first. It is possible that after this call is completed, there
   * is no time left for other actions. When the specified {@link Vehicle} has
   * no pending operation nothing will happen.
   * @param vehicle {@link Vehicle}
   * @param time {@link TimeLapse} that is available for performing the actions.
   */
  protected abstract void continuePreviousActions(Vehicle vehicle,
    TimeLapse time);

  /**
   * Actual implementation of {@link #register(PDPObject)}.
   * @param object The object to register.
   * @return <code>true</code> when registration succeeded, <code>false</code>
   *         otherwise.
   */
  protected abstract boolean doRegister(PDPObject object);

  /**
   * Returns an unmodifiable view on the contents of the specified container.
   * @param container The container to inspect.
   * @return An unmodifiable collection.
   */
  public abstract ImmutableSet<Parcel> getContents(Container container);

  /**
   * Returns the size of the contents of the specified container, this is the
   * sum of the contents magnitudes.
   * @param container The container to inspect.
   * @return A <code>double</code> indicating the size of the contents.
   */
  public abstract double getContentsSize(Container container);

  /**
   * @param container The container to check.
   * @return The maximum capacity of the specified container.
   */
  public abstract double getContainerCapacity(Container container);

  /**
   * Attempts to pickup the specified {@link Parcel} into the specified
   * {@link Vehicle}. Preconditions:
   * <ul>
   * <li>{@link Vehicle} must be on
   * {@link com.github.rinde.rinsim.core.model.road.RoadModel}.</li>
   * <li>{@link Vehicle} must be registered in {@link DefaultPDPModel}.</li>
   * <li>{@link Vehicle} must be in {@link VehicleState#IDLE} state.</li>
   * <li>{@link Parcel} must be on
   * {@link com.github.rinde.rinsim.core.model.road.RoadModel}.</li>
   * <li>{@link Parcel} must be registered in {@link DefaultPDPModel}.</li>
   * <li>{@link Parcel} must be in {@link ParcelState#ANNOUNCED} or
   * {@link ParcelState#AVAILABLE} state.</li>
   * <li>{@link Vehicle} and {@link Parcel} must be at same position in
   * {@link com.github.rinde.rinsim.core.model.road.RoadModel}.</li>
   * <li>{@link Parcel} must fit in {@link Vehicle}.</li>
   * </ul>
   * If any of the preconditions is not met this method throws an
   * {@link IllegalArgumentException}.
   * <p>
   * When all preconditions are met, the pickup action is started indicated by
   * the dispatching of an {@link com.github.rinde.rinsim.event.Event} with type
   * {@link PDPModelEventType#START_PICKUP}. In case the specified
   * {@link TimeLapse} is not big enough to complete the pickup immediately the
   * action will be continued next tick. Note that this method does not, and in
   * fact, should not be called again in the next tick to continue the pickup.
   * The continued pickup is handled automatically, the effect is that the
   * {@link Vehicle} will receive less time (or no time at all) in its next
   * tick. When the pickup action is completed an
   * {@link com.github.rinde.rinsim.event.Event} with type
   * {@link PDPModelEventType#END_PICKUP} is dispatched. When done, the
   * {@link Parcel} will be contained by the {@link Vehicle}.
   * @param vehicle The {@link Vehicle} involved in pickup.
   * @param parcel The {@link Parcel} to pick up.
   * @param time The {@link TimeLapse} that is available for the action.
   */
  public abstract void pickup(Vehicle vehicle, Parcel parcel, TimeLapse time);

  /**
   * Attempts to drop the specified {@link Parcel} into the specified
   * {@link Vehicle}. Preconditions:
   * <ul>
   * <li>{@link Vehicle} must exist in
   * {@link com.github.rinde.rinsim.core.model.road.RoadModel}.</li>
   * <li>{@link Vehicle} must be in {@link VehicleState#IDLE} state.</li>
   * <li>{@link Vehicle} must contain the specified {@link Parcel}.</li>
   * </ul>
   * If any of the preconditions is not met this method throws an
   * {@link IllegalArgumentException}.
   * <p>
   * When all preconditions are met, the drop action is started indicated by the
   * dispatching of an {@link com.github.rinde.rinsim.event.Event} with type
   * {@link PDPModelEventType#START_DELIVERY}. If there is not enough time in
   * the specified {@link TimeLapse} to complete the dropping at once, the
   * action will be completed in the next tick. Note that this method does not,
   * and in fact, should not be called again in the next tick to continue the
   * dropping. The continued dropping is handled automatically, the effect is
   * that the {@link Vehicle} will receive less time (or no time at all) in its
   * next tick. When the dropping is completed an
   * {@link com.github.rinde.rinsim.event.Event} with type
   * {@link PDPModelEventType#END_DELIVERY} is dispatched. As a result the
   * {@link Vehicle} no longer contains the {@link Parcel} and the
   * {@link Parcel} is added to the
   * {@link com.github.rinde.rinsim.core.model.road.RoadModel} again.
   * @param vehicle The {@link Vehicle} that wishes to deliver a {@link Parcel}.
   * @param parcel The {@link Parcel} that is to be delivered.
   * @param time The {@link TimeLapse} that is available for delivery.
   */
  public abstract void drop(Vehicle vehicle, Parcel parcel, TimeLapse time);

  /**
   * The specified {@link Vehicle} attempts to deliver the {@link Parcel} at its
   * current location. Preconditions:
   * <ul>
   * <li>{@link Vehicle} must exist in
   * {@link com.github.rinde.rinsim.core.model.road.RoadModel}.</li>
   * <li>{@link Vehicle} must be in {@link VehicleState#IDLE} state.</li>
   * <li>{@link Vehicle} must contain the specified {@link Parcel}.</li>
   * <li>{@link Vehicle} must be at the position indicated by
   * {@link Parcel#getDestination()}.</li>
   * </ul>
   * If any of the preconditions is not met this method throws an
   * {@link IllegalArgumentException}.
   * <p>
   * When all preconditions are met the actual delivery is started, this is
   * indicated by the dispatching of an
   * {@link com.github.rinde.rinsim.event.Event} with
   * {@link PDPModelEventType#START_DELIVERY} type. If there is not enough time
   * in the specified {@link TimeLapse} to complete the delivery at once, the
   * action will be completed in the next tick. Note that this method does not,
   * and in fact, should not be called again in the next tick to continue the
   * delivery. The continued delivery is handled automatically, the effect is
   * that the {@link Vehicle} will receive less time (or no time at all) in its
   * next tick. When the delivery is completed an
   * {@link com.github.rinde.rinsim.event.Event} with type
   * {@link PDPModelEventType#END_DELIVERY} is dispatched. As a result the
   * {@link Vehicle} no longer contains the {@link Parcel} and the
   * {@link Parcel} is NOT added to the
   * {@link com.github.rinde.rinsim.core.model.road.RoadModel} again.
   * @param vehicle The {@link Vehicle} that wishes to deliver a {@link Parcel}.
   * @param parcel The {@link Parcel} that is to be delivered.
   * @param time The {@link TimeLapse} that is available for delivery.
   */
  public abstract void deliver(Vehicle vehicle, Parcel parcel, TimeLapse time);

  /**
   * This method is intended for {@link Parcel}s that wish to add themselves to
   * either a {@link Vehicle} or a {@link Depot}.
   * @param container The {@link Container} to which the specified
   *          {@link Parcel} is added.
   * @param parcel The {@link Parcel} that is added.
   */
  public abstract void addParcelIn(Container container, Parcel parcel);

  /**
   * @param state The state of the returned parcels.
   * @return All {@link Parcel}s which are in the specified state, or an empty
   *         collection if there are no parcels in the specified state.
   */
  public abstract Collection<Parcel> getParcels(ParcelState state);

  /**
   * @param states All returned parcels have one of the specified states.
   * @return All {@link Parcel}s which are in the specified state, or an empty
   *         collection if there are no parcels in the specified state.
   */
  public abstract Collection<Parcel> getParcels(ParcelState... states);

  /**
   * @return The set of known vehicles.
   */
  public abstract Set<Vehicle> getVehicles();

  /**
   * @param parcel The {@link Parcel} for which the state is checked.
   * @return The {@link ParcelState} of the specified {@link Parcel}.
   */
  public abstract ParcelState getParcelState(Parcel parcel);

  /**
   * @param vehicle The {@link Vehicle} for which the state is checked.
   * @return The {@link VehicleState} of the specified {@link Vehicle}.
   */
  public abstract VehicleState getVehicleState(Vehicle vehicle);

  // TODO create a similar method but with a parcel as key
  /**
   * @param vehicle The vehicle for which a
   *          {@link PDPModel.VehicleParcelActionInfo} is retrieved.
   * @return Information about either a pickup or a delivery process.
   * @throws IllegalArgumentException if the specified vehicle is not in
   *           {@link VehicleState#DELIVERING} or in
   *           {@link VehicleState#PICKING_UP}.
   */
  public abstract PDPModel.VehicleParcelActionInfo getVehicleActionInfo(
    Vehicle vehicle);

  /**
   * @return The {@link EventAPI} used by this model. Events that are dispatched
   *         are instances of {@link PDPModelEvent}, the possible event types
   *         are listed in {@link PDPModelEventType}.
   */
  public abstract EventAPI getEventAPI();

  /**
   * Inspects the contents of the specified {@link Container} for existence of
   * the specified {@link Parcel} object.
   * @param container The container which is inspected.
   * @param parcel The parcel which we are checking.
   * @return <code>true</code> if the {@link Parcel} is contained in the
   *         {@link Container}, <code>false</code> otherwise.
   */
  public abstract boolean containerContains(Container container, Parcel parcel);

  /**
   * @return The {@link TimeWindowPolicy} that is used.
   */
  public abstract TimeWindowPolicy getTimeWindowPolicy();

  /**
   * Performs either a {@link #pickup(Vehicle, Parcel, TimeLapse)} or a
   * {@link #deliver(Vehicle, Parcel, TimeLapse)} operation depending on the
   * state of the vehicle and parcel.
   * @param vehicle The vehicle to perform the operation with.
   * @param parcel The parcel to perform the operation on.
   * @param time The time to spend on this operation.
   */
  public abstract void service(Vehicle vehicle, Parcel parcel, TimeLapse time);

  /**
   * The possible states a {@link Parcel} can be in.
   * @author Rinde van Lon
   */
  public enum ParcelState {

    /**
     * State that indicates that the {@link Parcel} is not yet available for
     * pickup but that it will be in the (near) future.
     */
    ANNOUNCED(false, false, false),

    /**
     * State that indicates that the {@link Parcel} is available for pickup.
     */
    AVAILABLE(false, false, false),

    /**
     * State that indicates that the {@link Parcel} is in the process of being
     * picked up.
     */
    PICKING_UP(false, false, true),

    /**
     * State that indicates that the {@link Parcel} is currently in the cargo of
     * a {@link Vehicle}.
     */
    IN_CARGO(true, false, false),

    /**
     * State that indicates that the {@link Parcel} is in the process of being
     * delivered.
     */
    DELIVERING(true, false, true),

    /**
     * State that indicates that the {@link Parcel} has been delivered.
     */
    DELIVERED(true, true, false);

    private final boolean picked;
    private final boolean delivered;
    private final boolean transition;

    ParcelState(boolean p, boolean d, boolean t) {
      picked = p;
      delivered = d;
      transition = t;
    }

    /**
     * @return <code>true</code> if the current state implies that the parcel is
     *         already picked up, <code>false</code> otherwise.
     */
    public boolean isPickedUp() {
      return picked;
    }

    /**
     * @return <code>true</code> if the current state implies that the parcel is
     *         already delivered, <code>false</code> otherwise.
     */
    public boolean isDelivered() {
      return delivered;
    }

    /**
     * @return <code>true</code> if the current state implies that the parcel is
     *         currently undergoing a transition (either loading or unloading),
     *         <code>false</code> otherwise.
     */
    public boolean isTransitionState() {
      return transition;
    }
  }

  /**
   * The possible states a {@link Vehicle} can be in.
   * @author Rinde van Lon
   */
  public enum VehicleState {
    /**
     * The 'normal' state, indicating that a {@link Vehicle} is neither in
     * {@link #PICKING_UP} nor in {@link #DELIVERING} state.
     */
    IDLE,

    /**
     * State that indicates that the {@link Vehicle} is currently picking up a
     * {@link Parcel}.
     */
    PICKING_UP,

    /**
     * State that indicates that the {@link Vehicle} is currently delivering a
     * {@link Parcel}.
     */
    DELIVERING
  }

  /**
   * The possible {@link com.github.rinde.rinsim.event.Event} types that the
   * {@link DefaultPDPModel} dispatches.
   * @author Rinde van Lon
   */
  public enum PDPModelEventType {
    /**
     * Indicates the start of a pickup of a {@link Parcel} by a {@link Vehicle}.
     */
    START_PICKUP,

    /**
     * Indicates the end of a pickup of a {@link Parcel} by a {@link Vehicle}.
     */
    END_PICKUP,

    /**
     * Indicates the start of a delivery of a {@link Parcel} by a
     * {@link Vehicle}.
     */
    START_DELIVERY,

    /**
     * Indicates the end of a delivery of a {@link Parcel} by a {@link Vehicle}.
     */
    END_DELIVERY,

    /**
     * Indicates that a new {@link Parcel} has been added to the model.
     */
    NEW_PARCEL,

    /**
     * Indicates that a new {@link Vehicle} has been added to the model.
     */
    NEW_VEHICLE,

    /**
     * Indicates that a {@link Parcel} has become available. This means that it
     * switched state from {@link ParcelState#ANNOUNCED} state to
     * {@link ParcelState#AVAILABLE} state.
     */
    PARCEL_AVAILABLE;
  }

  /**
   * Value object containing information about either a pickup or a delivery
   * operation.
   * @author Rinde van Lon
   */
  public interface VehicleParcelActionInfo {

    /**
     * @return The time needed to complete this action.
     */
    long timeNeeded();

    /**
     * @return The vehicle performing the action.
     */
    Vehicle getVehicle();

    /**
     * @return The parcel that is participating in the action.
     */
    Parcel getParcel();
  }
}
