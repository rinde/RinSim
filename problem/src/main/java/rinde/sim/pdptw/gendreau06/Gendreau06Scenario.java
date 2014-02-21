/**
 * 
 */
package rinde.sim.pdptw.gendreau06;

import java.util.Collection;
import java.util.Set;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.DefaultPDPModel;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.twpolicy.TardyAllowedPolicy;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.SimulationInfo;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.StopCondition;
import rinde.sim.pdptw.common.DynamicPDPTWScenario;
import rinde.sim.pdptw.common.PDPRoadModel;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;
import rinde.sim.util.spec.Specification;
import rinde.sim.util.spec.Specification.ISpecification;

/**
 * 
 * The length of the scenario is a soft constraint. There is a pre defined
 * length of the day (either 4 hours or 7.5 hours), vehicles are allowed to
 * continue driving after the end of the day.
 * <p>
 * Once a vehicle is moving towards a Parcel it is obliged to service it. This
 * means that diversion is not allowed.
 * <p>
 * Distance is expressed in km, time is expressed in ms (the original format is
 * in seconds, however it allows fractions as such it was translated to ms),
 * speed is expressed as km/h.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class Gendreau06Scenario extends DynamicPDPTWScenario {
  private static final long serialVersionUID = 1386559671732721432L;

  private static final Point MIN = new Point(0, 0);
  private static final Point MAX = new Point(5, 5);
  private static final Measure<Double, Velocity> MAX_SPEED = Measure.valueOf(
      30d, NonSI.KILOMETERS_PER_HOUR);

  private final long tickSize;
  private final GendreauProblemClass problemClass;
  private final int instanceNumber;
  private final boolean allowDiversion;

  Gendreau06Scenario(Collection<? extends TimedEvent> pEvents,
      Set<Enum<?>> pSupportedTypes, long ts, GendreauProblemClass problemClass,
      int instanceNumber, boolean diversion) {
    super(pEvents, pSupportedTypes);
    tickSize = ts;
    this.problemClass = problemClass;
    this.instanceNumber = instanceNumber;
    allowDiversion = diversion;
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
  public ISpecification<SimulationInfo> getStopCondition() {
    return Specification.of(StopCondition.VEHICLES_DONE_AND_BACK_AT_DEPOT)
        .and(StopCondition.TIME_OUT_EVENT).build();
  }

  @Override
  public RoadModel createRoadModel() {
    return new PDPRoadModel(new PlaneRoadModel(MIN, MAX, getDistanceUnit(),
        MAX_SPEED), allowDiversion);
  }

  @Override
  public PDPModel createPDPModel() {
    return new DefaultPDPModel(new TardyAllowedPolicy());
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

  @Override
  public ProblemClass getProblemClass() {
    return problemClass;
  }

  @Override
  public String getProblemInstanceId() {
    return Integer.toString(instanceNumber);
  }
}
