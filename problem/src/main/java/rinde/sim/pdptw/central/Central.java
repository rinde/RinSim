/**
 * 
 */
package rinde.sim.pdptw.central;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import rinde.sim.core.Simulator;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.event.Event;
import rinde.sim.event.EventAPI;
import rinde.sim.event.EventDispatcher;
import rinde.sim.event.Listener;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.DefaultParcel;
import rinde.sim.pdptw.common.DynamicPDPTWProblem;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.Creator;
import rinde.sim.pdptw.common.DynamicPDPTWScenario;
import rinde.sim.pdptw.common.ObjectiveFunction;
import rinde.sim.pdptw.common.PDPRoadModel;
import rinde.sim.pdptw.common.RouteFollowingVehicle;
import rinde.sim.pdptw.common.StatsTracker.StatisticsDTO;

/**
 * A facade for RinSim which provides a centralized interface such that
 * {@link Solver} instances can solve {@link DynamicPDPTWScenario}s.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class Central {

    private Central() {}

    /**
     * Runs the specified solver on the specified scenario in a simulation. The
     * result is evaluated using the objective function.
     * @param scenario The scenario which defines the problem to solve.
     * @param solver The {@link Solver}.
     * @param objFunc The objective function which is used to evaluate the
     *            solver.
     * @param showGui If <code>true</code> the gui will be fired up.
     * @throws IllegalStateException if the resulting statistics are not valid
     *             according to the objective function:
     *             {@link ObjectiveFunction#isValidResult(StatisticsDTO)}.
     * @return The statistics that were gathered during the simulation.
     */
    public static StatisticsDTO solve(DynamicPDPTWScenario scenario,
            final Solver solver, ObjectiveFunction objFunc, boolean showGui) {
        final Unit<Duration> timeUnit = scenario.getTimeUnit();
        final Unit<Velocity> speedUnit = scenario.getSpeedUnit();
        final Unit<Length> distUnit = scenario.getDistanceUnit();

        final ReceiverModel<RouteFollowingVehicle> driverReceiver =
                ReceiverModel.create(RouteFollowingVehicle.class);
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
                    return sim.register(new RouteFollowingVehicle(
                            event.vehicleDTO, timeUnit, speedUnit, distUnit));
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
                final Iterator<RouteFollowingVehicle> drivers =
                        rm.getObjectsOfType(RouteFollowingVehicle.class)
                                .iterator();
                while (drivers.hasNext()) {
                    drivers.next().setRoute(routes.next());
                }
            }
        }, ReceiveEvent.RECEIVE);
        final StatisticsDTO result = problemInstance.simulate();

        checkState(objFunc.isValidResult(result),
            "The simulation did not result in a valid result: %s.", result);
        return result;
    }

    private enum ReceiveEvent {
        RECEIVE;
    }

    private static final class ReceiverModel<T> implements Model<T> {

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
}
