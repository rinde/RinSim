/**
 * 
 */
package rinde.sim.problem.common;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import org.junit.Test;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.core.model.pdp.twpolicy.TardyAllowedPolicy;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.problem.common.DynamicPDPTWProblem.Creator;
import rinde.sim.problem.common.DynamicPDPTWProblem.SimulationInfo;
import rinde.sim.problem.common.DynamicPDPTWProblem.StopCondition;
import rinde.sim.problem.common.StatsTracker.StatisticsDTO;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class TestDynamicPDPTWProblem {

    protected static final Creator<AddVehicleEvent> DUMMY_ADD_VEHICLE_EVENT_CREATOR =
            new Creator<AddVehicleEvent>() {
                @Override
                public boolean create(Simulator sim, AddVehicleEvent event) {
                    return true;
                }
            };

    /**
     * Checks for the absence of a creator for AddVehicleEvent.
     */
    @Test(expected = IllegalStateException.class)
    public void noVehicleCreator() {
        final Set<TimedEvent> events =
                newHashSet(new TimedEvent(PDPScenarioEvent.ADD_DEPOT, 10));
        new DynamicPDPTWProblem(new DummyScenario(events), 123).simulate();
    }

    @Test
    public void testStopCondition() {
        final Set<TimedEvent> events =
                newHashSet(new TimedEvent(PDPScenarioEvent.ADD_DEPOT, 10));
        final DynamicPDPTWProblem prob =
                new DynamicPDPTWProblem(new DummyScenario(events), 123);
        prob.addCreator(AddVehicleEvent.class, DUMMY_ADD_VEHICLE_EVENT_CREATOR);

        prob.addStopCondition(new TimeStopCondition(4));
        final StatisticsDTO stats = prob.simulate();

        assertEquals(5, stats.simulationTime);
    }

    class TimeStopCondition extends StopCondition {
        protected final long time;

        public TimeStopCondition(long t) {
            time = t;
        }

        @Override
        public boolean isSatisfiedBy(SimulationInfo context) {
            return context.stats.simulationTime == time;
        }
    }

    class DummyScenario extends DynamicPDPTWScenario {

        public DummyScenario(Set<TimedEvent> events) {
            super(events, new HashSet<Enum<?>>(
                    java.util.Arrays.asList(PDPScenarioEvent.values())));
        }

        @Override
        public TimeWindow getTimeWindow() {
            return TimeWindow.ALWAYS;
        }

        @Override
        public long getTickSize() {
            return 1;
        }

        @Override
        public StopCondition getStopCondition() {
            return StopCondition.TIME_OUT_EVENT;
        }

        @Override
        public RoadModel createRoadModel() {
            return new PlaneRoadModel(new Point(0, 0), new Point(10, 10),
                    false, 1d);
        }

        @Override
        public PDPModel createPDPModel() {
            return new PDPModel(new TardyAllowedPolicy());
        }

        @Override
        public Unit<Duration> getTimeUnit() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Unit<Velocity> getSpeedUnit() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Unit<Length> getDistanceUnit() {
            // TODO Auto-generated method stub
            return null;
        }

    }

}
