package rinde.sim.pdptw.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newLinkedList;
import static java.util.Collections.unmodifiableCollection;

import java.math.RoundingMode;
import java.util.Collection;
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
 * {@link #setRoute(Collection)}.
 * <p>
 * This vehicle uses a strategy that postpones travelling towards a parcel such
 * that any waiting time <i>at the parcel's site is minimized</i>.
 * <p>
 * If it is the end of the day (as defined by {@link #isEndOfDay(TimeLapse)})
 * and the route is empty, the vehicle will automatically return to the depot.
 * 
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
   * The service state: {@link Service}.
   */
  protected final Service serviceState;

  Queue<DefaultParcel> route;
  Optional<DefaultDepot> depot;
  Optional<TimeLapse> currentTime;
  private Optional<Measure<Double, Velocity>> speed;

  /**
   * Initializes the vehicle.
   * @param pDto The {@link VehicleDTO} that defines this vehicle.
   */
  public RouteFollowingVehicle(VehicleDTO pDto) {
    super(pDto);
    depot = Optional.absent();
    speed = Optional.absent();
    route = newLinkedList();
    currentTime = Optional.absent();

    waitState = new Wait();
    gotoState = new Goto();
    serviceState = new Service();
    stateMachine = StateMachine.create(waitState)
        .addTransition(waitState, StateEvent.GOTO, gotoState)
        .addTransition(gotoState, StateEvent.ARRIVED, serviceState)
        .addTransition(serviceState, StateEvent.DONE, waitState).build();
  }

  /**
   * Change the route this vehicle is following. Parcels that have not yet been
   * picked up can at maximum occur twice in the route, parcels that have been
   * picked up can occur at maximum once in the route. Parcels that are
   * delivered may not occur in the route. This method copies the elements from
   * the {@link Collection} in the order as specified by this collection.
   * @param r The route to set.
   */
  public void setRoute(Collection<DefaultParcel> r) {
    route = newLinkedList(r);
  }

  @Override
  public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
    super.initRoadPDP(pRoadModel, pPdpModel);

    final Set<DefaultDepot> depots = roadModel.get().getObjectsOfType(
        DefaultDepot.class);
    checkArgument(depots.size() == 1,
        "This vehicle requires exactly 1 depot, found %s depots.",
        depots.size());
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
   * The event types of the state machine.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  protected enum StateEvent {
    /**
     * Indicates that waiting is over, the vehicle is going to a parcel.
     */
    GOTO,
    /**
     * Indicates that the vehicle has arrived at a service location.
     */
    ARRIVED,
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

    @Nullable
    @Override
    public StateEvent handle(StateEvent event, RouteFollowingVehicle context) {
      if (route.peek() != null) {
        if (!isTooEarly(route.peek(), currentTime.get())) {
          return StateEvent.GOTO;
        }
      }
      // check if it is time to go back to the depot
      else if (currentTime.get().hasTimeLeft() && route.isEmpty()
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

    @Nullable
    @Override
    public StateEvent handle(StateEvent event, RouteFollowingVehicle context) {
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
   * State responsible for servicing a parcel.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  protected class Service extends AbstractTruckState {
    /**
     * Indicates whether servicing has started upon entry in the state.
     */
    protected boolean startedServicing;

    /**
     * New instance.
     */
    protected Service() {}

    @Override
    public void onEntry(StateEvent event, RouteFollowingVehicle context) {
      checkArgument(currentTime.get().hasTimeLeft(),
          "We can't go into service state when there is no time left to consume.");
      startedServicing = false;

      service(context);
    }

    private void service(RouteFollowingVehicle context) {
      final PDPModel pm = pdpModel.get();
      final TimeLapse time = currentTime.get();
      final DefaultParcel cur = route.element();

      // if parcel is not ready yet, wait
      final boolean pickup = !pm.getContents(context).contains(cur);
      final long timeUntilReady = (pickup ? cur.dto.pickupTimeWindow.begin
          : cur.dto.deliveryTimeWindow.begin) - time.getTime();
      boolean canStart = true;
      if (timeUntilReady > 0) {
        if (time.getTimeLeft() < timeUntilReady) {
          // in this case we can not yet start
          // servicing
          time.consumeAll();
          canStart = false;
        } else {
          time.consume(timeUntilReady);
        }
      }

      if (canStart) {
        // parcel is ready, service
        pm.service(context, cur, time);
        route.remove();
        startedServicing = true;
      }
    }

    @Nullable
    @Override
    public StateEvent handle(StateEvent event, RouteFollowingVehicle context) {
      if (!startedServicing && currentTime.get().hasTimeLeft()) {
        service(context);
      } else if (currentTime.get().hasTimeLeft()) {
        return StateEvent.DONE;
      }
      return null;
    }
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
        parcelState);
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
   * @return the route
   */
  protected Collection<DefaultParcel> getRoute() {
    return unmodifiableCollection(route);
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
}
