/**
 * 
 */
package rinde.sim.problem.gendreau06;

import java.util.Collection;
import java.util.Set;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.twpolicy.TardyAllowedPolicy;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.problem.common.DynamicPDPTWProblem.StopCondition;
import rinde.sim.problem.common.DynamicPDPTWScenario;
import rinde.sim.problem.common.StrictRoadModel;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

/**
 * 
 * The length of the scenario is a soft constraint. There is a pre defined
 * length of the day (either 4 hours or 7.5 hours), vehicles are allowed to
 * continue driving after the end of the day.
 * 
 * Once a vehicle is moving towards a Parcel it is obliged to service it. This
 * means that diversion is not allowed.
 * 
 * Distance is expressed in km, time is expressed in ms (the original format is
 * in seconds, however it allows fractions as such it was translated to ms),
 * speed is expressed as km/h.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class Gendreau06Scenario extends DynamicPDPTWScenario {
    private static final long serialVersionUID = 1386559671732721432L;

    protected final long tickSize;

    protected static final Point min = new Point(0, 0);
    protected static final Point max = new Point(5, 5);
    protected static final double maxSpeed = 30.0;

    protected Gendreau06Scenario(Collection<? extends TimedEvent> pEvents,
            Set<Enum<?>> pSupportedTypes, long ts) {
        super(pEvents, pSupportedTypes);
        tickSize = ts;
    }

    protected Gendreau06Scenario(Collection<? extends TimedEvent> pEvents,
            Set<Enum<?>> pSupportedTypes) {
        this(pEvents, pSupportedTypes, 1000L);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this,
            ToStringStyle.MULTI_LINE_STYLE);
    }

    @Override
    public TimeWindow getTimeWindow() {
        return TimeWindow.ALWAYS;
    }

    @Override
    public long getTickSize() {
        return tickSize;
    }

    @Override
    public StopCondition getStopCondition() {
        return StopCondition.VEHICLES_BACK_AT_DEPOT;
    }

    @Override
    public RoadModel createRoadModel() {
        return new StrictRoadModel(
                new PlaneRoadModel(min, max, true, maxSpeed), false);
    }

    @Override
    public PDPModel createPDPModel() {
        return new PDPModel(new TardyAllowedPolicy());
    }

    @Override
    public Unit<Duration> getTimeUnit() {
        return SI.MILLI(SI.SECOND);
    }

    @Override
    public Unit<Velocity> getSpeedUnit() {
        return NonSI.KILOMETERS_PER_HOUR;
    }

    @Override
    public Unit<Length> getDistanceUnit() {
        return SI.KILOMETER;
    }
}
