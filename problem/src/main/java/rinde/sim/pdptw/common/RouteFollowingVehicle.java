package rinde.sim.pdptw.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newLinkedList;

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

    @Nullable
    private Queue<DefaultParcel> route;
    @Nullable
    private DefaultDepot depot;
    private final Unit<Duration> timeUnit;
    final Measure<Double, Velocity> speed;
    private final Unit<Length> distUnit;

    /**
     * Initializes the vehicle.
     * @param pDto The {@link VehicleDTO} that defines this vehicle.
     * @param tu The time unit.
     * @param su The speed unit.
     * @param du The distance unit.
     */
    public RouteFollowingVehicle(VehicleDTO pDto, Unit<Duration> tu,
            Unit<Velocity> su, Unit<Length> du) {
        super(pDto);
        timeUnit = tu;
        distUnit = du;
        speed = Measure.valueOf(getSpeed(), su);
    }

    /**
     * Change the route this vehicle is following. Parcels that have not yet
     * been picked up can at maximum occur twice in the route, parcels that have
     * been picked up can occur at maximum once in the route. Parcels that are
     * delivered may not occur in the route. This method copies the elements
     * from the {@link Collection} in the order as specified by this collection.
     * @param r The route to set.
     */
    public void setRoute(Collection<DefaultParcel> r) {
        route = newLinkedList(r);
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
        super.initRoadPDP(pRoadModel, pPdpModel);

        final Set<DefaultDepot> depots =
                roadModel.getObjectsOfType(DefaultDepot.class);
        checkArgument(depots.size() == 1,
            "This vehicle requires exactly 1 depot, found %s depots.",
            depots.size());
        depot = depots.iterator().next();
    }

    /**
     * This method can optionally be overridden to change route of this vehicle
     * by calling {@link #setRoute(Collection)} from within this method.
     * @param time The current time.
     */
    protected void preTick(TimeLapse time) {}

    @Override
    protected final void tickImpl(TimeLapse time) {
        preTick(time);
        final Queue<DefaultParcel> r = route;
        if (r != null && !r.isEmpty()) {
            while (time.hasTimeLeft() && r.peek() != null) {
                final DefaultParcel cur = r.element();
                // if leaving now would mean we are too early, wait
                if (isTooEarly(cur, time)) {
                    time.consumeAll();
                } else {
                    // if not there yet, go there
                    if (!roadModel.equalPosition(this, cur)) {
                        roadModel.moveTo(this, cur, time);
                    }
                    // if arrived
                    if (roadModel.equalPosition(this, cur)
                            && time.hasTimeLeft()) {
                        // if parcel is not ready yet, wait
                        final boolean pickup =
                                !pdpModel.getContents(this).contains(cur);
                        final long timeUntilReady =
                                (pickup ? cur.dto.pickupTimeWindow.begin
                                        : cur.dto.deliveryTimeWindow.begin)
                                        - time.getTime();

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
                            pdpModel.service(this, cur, time);
                            r.remove();
                        }
                    }
                }
            }
        }
        if (time.hasTimeLeft() && (r == null || r.isEmpty())
                && isEndOfDay(time) && !roadModel.equalPosition(this, depot)) {
            roadModel.moveTo(this, depot, time);
        }
    }

    /**
     * Calculates the arrival time of this vehicle at the parcel if it were to
     * leave right now (where now is indicated by the specified
     * {@link TimeLapse}. If the arrival time at the parcel is such that it
     * <i>can not</i> start servicing the parcel in the next tick, this method
     * returns <code>true</code>, and <code>false</code> otherwise. If the
     * parcel is in state {@link ParcelState#AVAILABLE} the vehicle can not be
     * too early, in this case <code>false</code> is always returned.
     * @param p The parcel to check travel time to.
     * @param time The current time.
     * @return <code>true</code> when arrival at the specified parcel is too
     *         early, <code>false</code> otherwise.
     */
    protected boolean isTooEarly(Parcel p, TimeLapse time) {
        final ParcelState parcelState = pdpModel.getParcelState(p);
        checkArgument(
            !parcelState.isTransitionState() && !parcelState.isDelivered(),
            parcelState);

        final boolean isPickup = !parcelState.isPickedUp();

        // if it is available, we know we can't be too early
        if (isPickup && parcelState == ParcelState.AVAILABLE) {
            return false;
        }

        final Point loc =
                isPickup ? ((DefaultParcel) p).dto.pickupLocation : p
                        .getDestination();
        final long travelTime = computeTravelTimeTo(loc);
        final long timeUntilAvailable =
                (isPickup ? p.getPickupTimeWindow().begin : p
                        .getDeliveryTimeWindow().begin) - time.getTime();

        // compute how many ticks from now the parcel will be available
        long ticksUntilAvailable = 0;
        if (time.getTimeLeft() < timeUntilAvailable) {
            ticksUntilAvailable =
                    DoubleMath.roundToLong(
                        (timeUntilAvailable - time.getTimeLeft())
                                / (double) time.getTimeStep(),
                        RoundingMode.FLOOR);
        }

        // compute how many ticks from now we arrive at the parcel
        long ticksUntilArrival = 0;
        if (time.getTimeLeft() < travelTime) {
            ticksUntilArrival =
                    DoubleMath.roundToLong((travelTime - time.getTimeLeft())
                            / (double) time.getTimeStep(), RoundingMode.FLOOR);
        }
        return ticksUntilArrival < ticksUntilAvailable;
    }

    /**
     * Computes the travel time for this vehicle to any point.
     * @param p The point to calculate travel time to.
     * @return The travel time in the used time unit.
     */
    protected long computeTravelTimeTo(Point p) {
        final Measure<Double, Length> distance =
                Measure.valueOf(Point.distance(roadModel.getPosition(this), p),
                    distUnit);
        return DoubleMath.roundToLong(
            ArraysSolvers.computeTravelTime(speed, distance, timeUnit),
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
        final long travelTime =
                computeTravelTimeTo(roadModel.getPosition(depot));
        return time.getEndTime() - 1 >= dto.availabilityTimeWindow.end
                - travelTime;
    }
}
