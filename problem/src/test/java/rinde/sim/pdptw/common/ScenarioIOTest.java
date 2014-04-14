package rinde.sim.pdptw.common;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.junit.Test;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.SimulationInfo;
import rinde.sim.pdptw.common.DynamicPDPTWScenario.ProblemClass;
import rinde.sim.scenario.ScenarioBuilder;
import rinde.sim.scenario.ScenarioBuilder.ScenarioCreator;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

public class ScenarioIOTest {

  @Test
  public void test() {
    final ScenarioBuilder sb = new ScenarioBuilder(PDPScenarioEvent.ADD_DEPOT,
        PDPScenarioEvent.ADD_PARCEL, PDPScenarioEvent.ADD_VEHICLE,
        PDPScenarioEvent.TIME_OUT);

    sb.addEvent(new AddVehicleEvent(100, new VehicleDTO(new Point(7, 7), 7d, 2,
        new TimeWindow(0, 1000L))));
    sb.addEvent(new AddDepotEvent(76, new Point(3, 3)));
    sb.addEvent(new AddVehicleEvent(125, new VehicleDTO(new Point(6, 9), 3d, 1,
        new TimeWindow(500, 10000L))));
    sb.addEvent(new AddParcelEvent(new ParcelDTO(new Point(0, 0), new Point(1,
        1), new TimeWindow(2500, 10000), new TimeWindow(5000, 10000), 0, 2400,
        200, 800)));
    sb.addEvent(new TimedEvent(PDPScenarioEvent.TIME_OUT, 200000));

    final TestScenario s = sb.build(new ScenarioCreator<TestScenario>() {
      @Override
      public TestScenario create(List<TimedEvent> eventList,
          Set<Enum<?>> eventTypes) {
        return new TestScenario(eventList, new TimeWindow(0, 1000), 1000L, SI
            .MILLI(SI.SECOND), TestProblemClass.TEST);
      }
    });

    final String res = ScenarioIO.write(s);
    assertEquals(s, ScenarioIO.read(res, TestScenario.class));
  }

  enum TestProblemClass implements ProblemClass {
    TEST;

    @Override
    public String getId() {
      return name();
    }
  }

  static class TestScenario extends DynamicPDPTWScenario {
    private static final long serialVersionUID = -3313653909815120873L;
    final TimeWindow timeWindow;
    final long tickSize;
    final Unit<Duration> timeUnit;
    final ProblemClass problemClass;

    public TestScenario(Collection<? extends TimedEvent> events, TimeWindow tw,
        long ts, Unit<Duration> tu, ProblemClass pc) {
      super(events, ImmutableSet.<Enum<?>> copyOf(PDPScenarioEvent.values()));
      timeWindow = tw;
      tickSize = ts;
      timeUnit = tu;
      problemClass = pc;
    }

    @Override
    public RoadModel createRoadModel() {
      throw new UnsupportedOperationException();
    }

    @Override
    public PDPModel createPDPModel() {
      throw new UnsupportedOperationException();
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
      throw new UnsupportedOperationException();
    }

    @Override
    public Unit<Duration> getTimeUnit() {
      return timeUnit;
    }

    @Override
    public Unit<Velocity> getSpeedUnit() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Unit<Length> getDistanceUnit() {
      throw new UnsupportedOperationException();
    }

    @Override
    public ProblemClass getProblemClass() {
      return problemClass;
    }

    @Override
    public String getProblemInstanceId() {
      throw new UnsupportedOperationException();
    }

  }
}
