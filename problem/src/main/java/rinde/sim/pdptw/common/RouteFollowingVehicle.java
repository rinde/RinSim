package rinde.sim.pdptw.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newLinkedList;
import static java.util.Collections.unmodifiableCollection;

import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPModel.ParcelState;
import rinde.sim.core.model.pdp.PDPModel.VehicleState;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.pdptw.central.arrays.ArraysSolvers;
import rinde.sim.util.fsm.AbstractState;
import rinde.sim.util.fsm.StateMachine;

import com.google.common.base.Optional;
import com.google.common.math.DoubleMath;

/**
 * A simple vehicle implementation that follows a route comprised of
 * {@link DefaultParcel}s. At every stop in the route, the corresponding parcel
 * is serviced (either picked up or delivered). The route can be set via
 * {@link #setRoute(Collection)}. The vehicle attempts route diversion when the
 * underlying {@link PDPRoadModel} allows it, otherwise it will change its route
 * at the next possible instant.
 * <p>
 * This vehicle uses a strategy that postpones travelling towards a parcel such
 * that any waiting time <i>at the parcel's site is minimized</i>.
 * <p>
 * If it is the end of the day (as defined by {@link #isEndOfDay(TimeLapse)})
 * and the route is empty, the vehicle will automatically return to the depot.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class RouteFollowingVehicle extends DefaultVehicle {

  /**
   * The state machine that defines the states and the allowed transitions
   * between them.
   */
  protected final StateMachine<StateEvent, RouteFollowingVehicle> stateMachine;

  /**
   * The wait state: {@link Wait}.
   */
  protected final Wait waitState;

  /**
   * The goto state: {@link Goto}.
   */
  protected final Goto gotoState;

  /**
   * The wait at service state: {@link WaitAtService}.
   */
  protected final WaitAtService waitForServiceState;

  /**
   * The service state: {@link Service}.
   */
  protected final Service serviceState;

  Queue<DefaultParcel> route;
  Optional<? extends Queue<DefaultParcel>> newRoute;
  Optional<DefaultDepot> depot;
  Optional<TimeLapse> currentTime;
  boolean isDiversionAllowed;
  private Optional<Measure<Double, Velocity>> speed;

  private final boolean allowDelayedRouteChanges;

  /**
   * Initializes the vehicle.
   * @param pDto The {@link VehicleDTO} that defines this vehicle.
   * @param allowDelayedRouteChanging This boolean changes the behavior of the
   *          {@link #setRoute(Collection)} method.
   */
  public RouteFollowingVehicle(VehicleDTO pDto,
      boolean allowDelayedRouteChanging) {
    super(pDto);
    depot = Optional.absent();
    speed = Optional.absent();
    route = newLinkedList();
    newRoute = Optional.absent();
    currentTime = Optional.absent();
    allowDelayedRouteChanges = allowDelayedRouteChanging;

    // TODO split up the Wait state into a GoHome or GotoDepot state.

    waitState = new Wait();
    gotoState = new Goto();
    waitForServiceState = new WaitAtService();
    serviceState = new Service();
    stateMachine = StateMachine
        .create(waitState)
        .addTransition(waitState, StateEvent.GOTO, gotoState)
        .addTransition(gotoState, StateEvent.NOGO, waitState)
        .addTransition(gotoState, StateEvent.ARRIVED, waitForServiceState)
        .addTransition(waitForServiceState, StateEvent.REROUTE, gotoState)
        .addTransition(waitForServiceState, StateEvent.READY_TO_SERVICE,
            serviceState)
        .addTransition(serviceState, StateEvent.DONE, waitState).build();
  }

  /**
   * Change the route this vehicle is following. The route must adhere to the
   * following requirements:
   * <ul>
   * <li>Parcels that have not yet been picked up can at maximum occur twice in
   * the route.</li>
   * <li>Parcels that have been picked up can occur at maximum once in the
   * route.</li>
   * <li>Parcels that are delivered may not occur in the route.</li>
   * </ul>
   * These requirements are <b>not</b> checked defensively! It is the callers
   * responsibility to make sure this is the case. Note that the underlying
   * models normally <i>should</i> throw exceptions whenever a vehicle attempts
   * to revisit an already delivered parcel.
   * <p>
   * In some case the models do not allow this vehicle to change its route
   * immediately. If this is the case the route is changed the next time this
   * vehicle enters its {@link #waitState}. If
   * <code>allowDelayedRouteChanging</code> is set to <code>false</code> any
   * attempts to to this will result in an runtime exception, in this case the
   * caller must ensure that a route is always changed immediately. The
   * situations when the route is changed immediately are:
   * <ul>
   * <li>If the vehicle is waiting.</li>
   * <li>If diversion is allowed and the vehicle is not currently servicing.</li>
   * <li>If the current route is empty.</li>
   * <li>If the first destination in the new route equals the first destination
   * of the current route.</li>
   * </ul>
   * @param r The route to set. The elements are copied from the
   *          {@link Collection} using its iteration order.
   */
  public void setRoute(Collection<DefaultParcel> r) {
    // note: the following checks can not detect if a parcel has been set to
    // multiple vehicles at the same time
    for (final DefaultParcel dp : r) {
      final ParcelState state = pdpModel.get().getParcelState(dp);
      checkArgument(
          !state.isDelivered(),
          "A parcel that is already delivered can not be part of a route. Parcel %s in route %s.",
          dp, r);
      if (state.isTransitionState()) {
        if (state == ParcelState.PICKING_UP) {
          checkArgument(
              pdpModel.get().getVehicleState(this) == VehicleState.PICKING_UP,
              "When a parcel in the route is in PICKING UP state the vehicle must also be in that state.");
        } else {
          checkArgument(
              pdpModel.get().getVehicleState(this) == VehicleState.DELIVERING,
              "When a parcel in the route is in DELIVERING state the vehicle must also be in that state.");
        }
        checkArgument(
            pdpModel.get().getVehicleActionInfo(this).getParcel() == dp,
            "A parcel in the route that is being serviced should be serviced by this truck. This truck is servicing %s.",
            pdpModel.get().getVehicleActionInfo(this).getParcel());
      }

      final int frequency = Collections.frequency(r, dp);
      if (state.isPickedUp()) {
        checkArgument(pdpModel.get().getContents(this).contains(dp),
            "A parcel that is in cargo state must be in cargo of this vehicle.");
        checkArgument(
            frequency <= 1,
            "A parcel that is in cargo may not occur more than once in a route, found %s instance(s).",
            frequency);
      } else {
        checkArgument(
            frequency <= 2,
            "A parcel that is available may not occur more than twice in a route, found %s instance(s).",
            frequency);
      }
    }

    if (stateMachine.stateIs(waitState) || route.isEmpty()
        || (isDiversionAllowed && !stateMachine.stateIs(serviceState))
        || firstEqualsFirstInRoute(r)) {
      route = newLinkedList(r);
      newRoute = Optional.absent();
    } else {
      checkArgument(allowDelayedRouteChanges,
          "Delayed route changes are not allowed, rejected route: %s.", r);
      newRoute = Optional.of(newLinkedList(r));
    }
  }

  /**
   * @return The route that is currently being followed.
   */
  public Collection<DefaultParcel> getRoute() {
    return unmodifiableCollection(route);
  }

  boolean firstEqualsFirstInRoute(Collection<DefaultParcel> r) {
    return !r.isEmpty() && !route.isEmpty()
        && r.iterator().next().equals(route.element());
  }

  @Override
  public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
    super.initRoadPDP(pRoadModel, pPdpModel);
    final Set<DefaultDepot> depots = roadModel.get().getObjectsOfType(
        DefaultDepot.class);
    checkArgument(depots.size() == 1,
        "This vehicle requires exactly 1 depot, found %s depots.",
        depots.size());
    checkArgument(roadModel.get() instanceof PDPRoadModel,
        "This vehicle requires the PDPRoadModel.");
    isDiversionAllowed = ((PDPRoadModel) roadModel.get())
        .isVehicleDiversionAllowed();
    depot = Optional.of(depots.iterator().next());
    speed = Optional.of(Measure.valueOf(getSpeed(), roadModel.get()
        .getSpeedUnit()));
  }

  /**
   * This method can optionally be overridden to change route of this vehicle by
   * calling {@link #setRoute(Collection)} from within this method.
   * @param time The current time.
   */
  protected void preTick(TimeLapse time) {}

  @Override
  protected final void tickImpl(TimeLapse time) {
    currentTime = Optional.of(time);
    preTick(time);
    stateMachine.handle(this);
  }

  /**
   * Check if leaving in the specified {@link TimeLapse} to the specified
   * {@link Parcel} would mean a too early arrival time. When this method
   * returns <code>true</code> it is not necessary to leave already, when
   * <code>false</code> is returned the vehicle should leave as soon as
   * possible.
   * <p>
   * Calculates the latest time to leave (lttl) to be just in time at the parcel
   * location. In case lttl is in this {@link TimeLapse} or has already passed,
   * this method returns <code>false</code>, returns <code>true</code>
   * otherwise.
   * @param p The parcel to travel to.
   * @param time The current time.
   * @return <code>true</code> when leaving in this tick would mean arriving too
   *         early, <code>false</code> otherwise.
   */
  protected boolean isTooEarly(Parcel p, TimeLapse time) {
    final ParcelState parcelState = pdpModel.get().getParcelState(p);
    checkArgument(
        !parcelState.isTransitionState() && !parcelState.isDelivered(),
        "Parcel state may not be a transition state nor may it be delivered, it is %s.",
        parcelState, parcelState.isTransitionState() ? pdpModel.get()
            .getVehicleActionInfo(this).timeNeeded() : null);
    final boolean isPickup = !parcelState.isPickedUp();
    // if it is available, we know we can't be too early
    if (isPickup && parcelState == ParcelState.AVAILABLE) {
      return false;
    }
    final Point loc = isPickup ? ((DefaultParcel) p).dto.pickupLocation : p
        .getDestination();
    final long travelTime = computeTravelTimeTo(loc, time.getTimeUnit());
    final long openingTime = isPickup ? p.getPickupTimeWindow().begin : p
        .getDeliveryTimeWindow().begin;
    final long latestTimeToLeave = openingTime - travelTime;
    return latestTimeToLeave >= time.getEndTime();
  }

  /**
   * Computes the travel time for this vehicle to any point.
   * @param p The point to calculate travel time to.
   * @param timeUnit The time unit used in the simulation.
   * @return The travel time in the used time unit.
   */
  protected long computeTravelTimeTo(Point p, Unit<Duration> timeUnit) {
    final Measure<Double, Length> distance = Measure.valueOf(Point.distance(
        roadModel.get().getPosition(this), p), roadModel.get()
        .getDistanceUnit());

    return DoubleMath.roundToLong(
        ArraysSolvers.computeTravelTime(speed.get(), distance, timeUnit),
        RoundingMode.CEILING);
  }

  /**
   * @param time The time to use as 'now'.
   * @return <code>true</code> if it is the end of the day or if this vehicle
   *         has to leave before the end of this tick to arrive back at the
   *         depot right before the end of the day, <code>false</code>
   *         otherwise.
   */
  protected boolean isEndOfDay(TimeLapse time) {
    final long travelTime = computeTravelTimeTo(
        roadModel.get().getPosition(depot.get()), time.getTimeUnit());
    return time.getEndTime() - 1 >= dto.availabilityTimeWindow.end - travelTime;
  }

  /**
   * @return the depot
   */
  protected DefaultDepot getDepot() {
    return depot.get();
  }

  /**
   * @return the currentTime
   */
  protected TimeLapse getCurrentTime() {
    return currentTime.get();
  }

  void checkCurrentParcelOwnership() {
    checkState(
        !pdpModel.get().getParcelState(route.peek()).isTransitionState(),
        "Parcel is already being serviced by another vehicle. Parcel state: %s",
        pdpModel.get().getParcelState(route.peek()));
  }

  /**
   * The event types of the state machine.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  protected enum StateEvent {
    /**
     * Indicates that waiting is over, the vehicle is going to a parcel.
     */
    GOTO,
    /**
     * Indicates that the vehicle no longer has a destination.
     */
    NOGO,
    /**
     * Indicates that the vehicle has arrived at a service location.
     */
    ARRIVED,
    /**
     * Indicates that the vehicle is at a service location and that the vehicle
     * and the parcel are both ready to start the servicing.
     */
    READY_TO_SERVICE,
    /**
     * Indicates that the vehicle has been waiting at a service point until it
     * became available but is now going to a new location.
     */
    REROUTE,
    /**
     * Indicates that servicing is finished.
     */
    DONE;
  }

  abstract class AbstractTruckState extends
      AbstractState<StateEvent, RouteFollowingVehicle> {
    @Override
    public String toString() {
      return this.getClass().getSimpleName();
    }
  }

  /**
   * Implementation of waiting state, is also responsible for driving back to
   * the depot.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  protected class Wait extends AbstractTruckState {

    /**
     * New instance.
     */
    protected Wait() {}

    @Override
    public void onEntry(StateEvent event, RouteFollowingVehicle context) {
      checkState(
          pdpModel.get().getVehicleState(context) == VehicleState.IDLE,
          "We can only be in Wait state when the vehicle is idle, vehicle is %s.",
          pdpModel.get().getVehicleState(context));
      if (event == StateEvent.NOGO) {
        checkArgument(isDiversionAllowed);
      }
      if (context.newRoute.isPresent()) {
        context.setRoute(context.newRoute.get());
      }
    }

    @Nullable
    @Override
    public StateEvent handle(StateEvent event, RouteFollowingVehicle context) {
      if (!route.isEmpty()) {
        checkCurrentParcelOwnership();
        if (!isTooEarly(route.peek(), currentTime.get())) {
          return StateEvent.GOTO;
        }
        // else it is too early, and we do nothing
      }
      // check if it is time to go back to the depot
      else if (currentTime.get().hasTimeLeft() //
          && isEndOfDay(currentTime.get())
          && !roadModel.get().equalPosition(context, depot.get())) {
        roadModel.get().moveTo(context, depot.get(), currentTime.get());
      }
      currentTime.get().consumeAll();
      return null;
    }
  }

  /**
   * State responsible for moving to a service location.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  protected class Goto extends AbstractTruckState {
    /**
     * New instance.
     */
    protected Goto() {}

    @Override
    public void onEntry(StateEvent event, RouteFollowingVehicle context) {
      if (event == StateEvent.REROUTE) {
        checkArgument(isDiversionAllowed);
      }
      checkCurrentParcelOwnership();
    }

    @Nullable
    @Override
    public StateEvent handle(StateEvent event, RouteFollowingVehicle context) {
      if (route.isEmpty()) {
        return StateEvent.NOGO;
      }
      final DefaultParcel cur = route.element();
      if (roadModel.get().equalPosition(context, cur)) {
        return StateEvent.ARRIVED;
      }
      roadModel.get().moveTo(context, cur, currentTime.get());
      if (roadModel.get().equalPosition(context, cur)
          && currentTime.get().hasTimeLeft()) {
        return StateEvent.ARRIVED;
      }
      return null;
    }
  }

  /**
   * State responsible for waiting at a service location to become available.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  protected class WaitAtService extends AbstractTruckState {
    /**
     * New instance.
     */
    protected WaitAtService() {}

    @Nullable
    @Override
    public StateEvent handle(StateEvent event, RouteFollowingVehicle context) {
      checkCurrentParcelOwnership();
      final PDPModel pm = pdpModel.get();
      final TimeLapse time = currentTime.get();
      final DefaultParcel cur = route.element();
      // we are not at the parcel's position, this means the next parcel has
      // changed in the mean time, so we have to reroute.
      if (!roadModel.get().equalPosition(context, cur)) {
        return StateEvent.REROUTE;
      }
      // if parcel is not ready yet, wait
      final boolean pickup = !pm.getContents(context).contains(cur);
      final long timeUntilReady = (pickup ? cur.dto.pickupTimeWindow.begin
          : cur.dto.deliveryTimeWindow.begin) - time.getTime();
      if (timeUntilReady > 0) {
        if (time.getTimeLeft() < timeUntilReady) {
          // in this case we can not yet start servicing
          time.consumeAll();
          return null;
        } else {
          time.consume(timeUntilReady);
        }
      }
      if (time.hasTimeLeft()) {
        return StateEvent.READY_TO_SERVICE;
      } else {
        return null;
      }
    }
  }

  /**
   * State responsible for servicing a parcel.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  protected class Service extends AbstractTruckState {
    /**
     * New instance.
     */
    protected Service() {}

    @Override
    public void onEntry(StateEvent event, RouteFollowingVehicle context) {
      pdpModel.get().service(context, route.peek(), currentTime.get());
    }

    @Nullable
    @Override
    public StateEvent handle(StateEvent event, RouteFollowingVehicle context) {
      if (currentTime.get().hasTimeLeft()) {
        route.remove();
        return StateEvent.DONE;
      }
      return null;
    }
  }
}
