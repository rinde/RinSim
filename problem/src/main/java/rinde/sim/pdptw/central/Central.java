/**
 * 
 */
package rinde.sim.pdptw.central;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;

import java.math.RoundingMode;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import rinde.sim.core.Simulator;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPModel.ParcelState;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.event.Event;
import rinde.sim.event.EventAPI;
import rinde.sim.event.EventDispatcher;
import rinde.sim.event.Listener;
import rinde.sim.pdptw.central.arrays.ArraysSolvers;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.DefaultDepot;
import rinde.sim.pdptw.common.DefaultParcel;
import rinde.sim.pdptw.common.DefaultVehicle;
import rinde.sim.pdptw.common.DynamicPDPTWProblem;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.Creator;
import rinde.sim.pdptw.common.DynamicPDPTWScenario;
import rinde.sim.pdptw.common.ObjectiveFunction;
import rinde.sim.pdptw.common.PDPRoadModel;
import rinde.sim.pdptw.common.StatsTracker.StatisticsDTO;
import rinde.sim.pdptw.common.VehicleDTO;

import com.google.common.math.DoubleMath;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class Central {

    public static void solve(DynamicPDPTWScenario scenario,
            final Solver solver, ObjectiveFunction objFunc, boolean showGui) {
        final Unit<Duration> timeUnit = scenario.getTimeUnit();
        final Unit<Velocity> speedUnit = scenario.getSpeedUnit();
        final Unit<Length> distUnit = scenario.getDistanceUnit();

        final ReceiverModel<RemoteDriver> driverReceiver =
                ReceiverModel.create(RemoteDriver.class);
        final ReceiverModel<DefaultParcel> parcelReceiver =
                ReceiverModel.create(DefaultParcel.class);
        final DynamicPDPTWProblem problemInstance =
                new DynamicPDPTWProblem(scenario, 0, driverReceiver,
                        parcelReceiver);

        if (showGui) {
            problemInstance.enableUI();
        }

        problemInstance.addCreator(AddVehicleEvent.class,
            new Creator<AddVehicleEvent>() {
                @Override
                public boolean create(Simulator sim, AddVehicleEvent event) {
                    return sim.register(new RemoteDriver(event.vehicleDTO,
                            timeUnit, speedUnit, distUnit));
                }
            });
        final Simulator sim = problemInstance.getSimulator();
        final PDPRoadModel rm =
                sim.getModelProvider().getModel(PDPRoadModel.class);
        final PDPModel pm = sim.getModelProvider().getModel(PDPModel.class);

        parcelReceiver.getEventAPI().addListener(new Listener() {
            @Override
            public void handleEvent(Event e) {
                final Iterator<Queue<DefaultParcel>> routes =
                        Solvers.solve(solver, rm, pm, sim.getCurrentTime(),
                            timeUnit, speedUnit, distUnit).iterator();
                final Iterator<RemoteDriver> drivers =
                        rm.getObjectsOfType(RemoteDriver.class).iterator();
                while (drivers.hasNext()) {
                    drivers.next().setRoute(routes.next());
                }
            }
        }, ReceiveEvent.RECEIVE);
        final StatisticsDTO result = problemInstance.simulate();

        checkState(objFunc.isValidResult(result),
            "The simulation did not result in a valid result: %s.", result);
        System.out.println(objFunc.printHumanReadableFormat(result));
    }

    private enum ReceiveEvent {
        RECEIVE;
    }

    private static class ReceiverModel<T> implements Model<T> {

        private final Class<T> type;
        private final List<T> objects;
        private final EventDispatcher eventDispatcher;

        private ReceiverModel(Class<T> type) {
            this.type = type;
            objects = newArrayList();
            eventDispatcher = new EventDispatcher(ReceiveEvent.RECEIVE);
        }

        @Override
        public boolean register(T element) {
            objects.add(element);
            eventDispatcher.dispatchEvent(new Event(ReceiveEvent.RECEIVE,
                    element));
            return false;
        }

        @Override
        public boolean unregister(T element) {
            return false;
        }

        @Override
        public Class<T> getSupportedType() {
            return type;
        }

        static <T> ReceiverModel<T> create(Class<T> type) {
            return new ReceiverModel<T>(type);
        }

        public EventAPI getEventAPI() {
            return eventDispatcher.getPublicEventAPI();
        }

    }

    private static class RemoteDriver extends DefaultVehicle {

        @Nullable
        private Queue<DefaultParcel> route;
        @Nullable
        private DefaultDepot depot;
        private final Unit<Duration> timeUnit;
        final Measure<Double, Velocity> speed;
        private final Unit<Length> distUnit;

        /**
         * @param pDto
         * @param tu
         * @param su
         * @param du
         */
        public RemoteDriver(VehicleDTO pDto, Unit<Duration> tu,
                Unit<Velocity> su, Unit<Length> du) {
            super(pDto);
            timeUnit = tu;
            distUnit = du;
            speed = Measure.valueOf(getSpeed(), su);
        }

        /**
         * @param r
         */
        public void setRoute(Queue<DefaultParcel> r) {
            // print(r);
            route = r;
        }

        static void print(Collection<DefaultParcel> ps) {
            final StringBuilder sb = new StringBuilder();
            for (final DefaultParcel p : ps) {
                sb.append(p.hashCode()).append(",");
            }
            System.out.println(sb.toString());
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

        @Override
        protected void tickImpl(TimeLapse time) {
            final Queue<DefaultParcel> r = route;
            if (r != null && !r.isEmpty()) {
                while (time.hasTimeLeft() && r.peek() != null) {
                    final DefaultParcel cur = r.element();

                    if (isTooEarly(cur, time)) {
                        // we have to wait
                        time.consumeAll();
                    } else {
                        // TODO check this code, why can we still be too early?
                        if (!roadModel.equalPosition(this, cur)) {
                            roadModel.moveTo(this, cur, time);
                        }
                        if (roadModel.equalPosition(this, cur)) {

                            // same can happen with delivery?
                            if (pdpModel.getParcelState(cur) == ParcelState.ANNOUNCED) {
                                // too early for pickup
                                // final long timeleft =
                                // cur.getPickupTimeWindow().begin
                                // - time.getTime();
                                // if (timeleft <= time.getTimeLeft()) {
                                // time.consume(timeleft);
                                // }
                                time.consumeAll();
                            } else {
                                pdpModel.service(this, cur, time);

                                r.remove();
                            }
                        }
                    }
                }
            }
            if (isEndOfDay(time) && !roadModel.equalPosition(this, depot)) {
                roadModel.moveTo(this, depot, time);
            }
        }

        // FIXME, check to see if correct, and to see if it makes sense to leave
        // one step earlier
        protected boolean isTooEarly(Parcel p, TimeLapse time) {
            final boolean isPickup =
                    pdpModel.getParcelState(p) != ParcelState.IN_CARGO;
            final Point loc =
                    isPickup ? ((DefaultParcel) p).dto.pickupLocation : p
                            .getDestination();
            final long travelTime = computeTravelTimeTo(loc);
            long timeUntilAvailable =
                    (isPickup ? p.getPickupTimeWindow().begin : p
                            .getDeliveryTimeWindow().begin)
                            - time.getStartTime();

            final long remainder = timeUntilAvailable % time.getTimeStep();
            if (remainder > 0) {
                timeUntilAvailable += time.getTimeStep() - remainder;
            }
            return timeUntilAvailable - travelTime > 0;
        }

        protected long computeTravelTimeTo(Point p) {
            final Measure<Double, Length> distance =
                    Measure.valueOf(
                        Point.distance(roadModel.getPosition(this), p),
                        distUnit);
            return DoubleMath.roundToLong(
                ArraysSolvers.computeTravelTime(speed, distance, timeUnit),
                RoundingMode.CEILING);
        }

        protected boolean isEndOfDay(TimeLapse time) {
            final long travelTime = computeTravelTimeTo(dto.startPosition);

            return time.hasTimeLeft()
                    && time.getTime() > dto.availabilityTimeWindow.end
                            - travelTime;
        }
    }
}
