/**
 * 
 */
package rinde.sim.problem.common;

import java.util.Collection;
import java.util.Set;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.problem.common.DynamicPDPTWProblem.StopCondition;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class DynamicPDPTWScenario extends Scenario {

    private static final long serialVersionUID = 7258024865764689371L;

    public DynamicPDPTWScenario() {
        super();
    }

    public DynamicPDPTWScenario(Collection<? extends TimedEvent> events,
            Set<Enum<?>> supportedTypes) {
        super(events, supportedTypes);
    }

    public abstract RoadModel createRoadModel();

    public abstract PDPModel createPDPModel();

    public abstract TimeWindow getTimeWindow();

    public abstract long getTickSize();

    public abstract StopCondition getStopCondition();

    public abstract Unit<Duration> getTimeUnit();

    public abstract Unit<Velocity> getSpeedUnit();

    public abstract Unit<Length> getDistanceUnit();

}
