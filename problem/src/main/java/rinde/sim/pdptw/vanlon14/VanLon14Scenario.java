package rinde.sim.pdptw.vanlon14;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.Set;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.pdp.DefaultPDPModel;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.SimulationInfo;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.StopCondition;
import rinde.sim.pdptw.common.PDPRoadModel;
import rinde.sim.pdptw.scenario.PDPScenario;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class VanLon14Scenario extends PDPScenario {

  private static final Measure<Double, Velocity> MAX_SPEED = Measure.valueOf(
      30d, NonSI.KILOMETERS_PER_HOUR);

  private static final Set<Enum<?>> SUPPORTED_TYPES = ImmutableSet
      .<Enum<?>> of(
          PDPScenarioEvent.ADD_DEPOT, PDPScenarioEvent.ADD_PARCEL,
          PDPScenarioEvent.ADD_VEHICLE, PDPScenarioEvent.TIME_OUT);

  private final Point min;
  private final Point max;
  private final TimeWindow timeWindow;
  private final long tickSize;
  private final ProblemClass problemClass;
  private final int instanceNumber;

  public VanLon14Scenario(Collection<? extends TimedEvent> events,
      TimeWindow tw, Point mi, Point ma, long ts, ProblemClass pc,
      int instanceNum) {
    super(events, SUPPORTED_TYPES);
    checkArgument(tw.begin == 0);
    checkArgument(tw.end > 0);
    checkArgument(ts > 0);

    min = mi;
    max = ma;
    timeWindow = tw;
    tickSize = ts;
    problemClass = pc;
    instanceNumber = instanceNum;
  }

  @Override
  public ImmutableList<Model<?>> createModels() {
    return ImmutableList.<Model<?>> of(createRoadModel(), createPDPModel());
  }

  RoadModel createRoadModel() {
    return new PDPRoadModel(new PlaneRoadModel(min, max, getDistanceUnit(),
        MAX_SPEED), false);
  }

  PDPModel createPDPModel() {
    return new DefaultPDPModel(TimeWindowPolicies.TARDY_ALLOWED);
  }

  @Override
  public TimeWindow getTimeWindow() {
    return timeWindow;
  }

  @Override
  public long getTickSize() {
    return tickSize;
  }

  @Override
  public Predicate<SimulationInfo> getStopCondition() {
    return Predicates.and(StopCondition.VEHICLES_DONE_AND_BACK_AT_DEPOT,
        StopCondition.TIME_OUT_EVENT);
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
