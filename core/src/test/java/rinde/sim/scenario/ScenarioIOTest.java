package rinde.sim.scenario;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.core.pdptw.AddDepotEvent;
import rinde.sim.core.pdptw.AddParcelEvent;
import rinde.sim.core.pdptw.AddVehicleEvent;
import rinde.sim.core.pdptw.ParcelDTO;
import rinde.sim.core.pdptw.VehicleDTO;
import rinde.sim.scenario.Scenario.ProblemClass;
import rinde.sim.scenario.Scenario.SimpleProblemClass;
import rinde.sim.util.TimeWindow;

/**
 * Scenario IO test.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class ScenarioIOTest {

  /**
   * Tests the equality of a serialized and deserialized scenario with the
   * original.
   */
  @Test
  public void test() {
    final Scenario.Builder sb = Scenario
        .builder(Scenario.DEFAULT_PROBLEM_CLASS);
    // (PDPScenarioEvent.ADD_DEPOT,
    // PDPScenarioEvent.ADD_PARCEL, PDPScenarioEvent.ADD_VEHICLE,
    // PDPScenarioEvent.TIME_OUT);

    sb.addEvent(new AddVehicleEvent(100, VehicleDTO.builder()
        .startPosition(new Point(7, 7))
        .speed(7d)
        .capacity(2)
        .availabilityTimeWindow(new TimeWindow(0, 1000L))
        .build()));
    sb.addEvent(new AddDepotEvent(76, new Point(3, 3)));

    sb.addEvent(new AddVehicleEvent(125, VehicleDTO.builder()
        .startPosition(new Point(6, 9))
        .speed(3d)
        .capacity(1)
        .availabilityTimeWindow(new TimeWindow(500, 10000L))
        .build()));

    sb.addEvent(new AddParcelEvent(ParcelDTO
        .builder(new Point(0, 0), new Point(1, 1))
        .pickupTimeWindow(new TimeWindow(2500, 10000))
        .deliveryTimeWindow(new TimeWindow(5000, 10000))
        .neededCapacity(0)
        .orderAnnounceTime(2400)
        .pickupDuration(200)
        .deliveryDuration(800)
        .build()));
    sb.addEvent(new TimedEvent(PDPScenarioEvent.TIME_OUT, 200000));

    final List<ProblemClass> pcs = asList(TestProblemClass.TEST,
        new SimpleProblemClass("hello"));
    for (final ProblemClass pc : pcs) {
      final Scenario s = sb.build();
      // new ScenarioCreator<TestScenario>() {
      // @Override
      // public TestScenario create(List<TimedEvent> eventList,
      // Set<Enum<?>> eventTypes) {
      // return new TestScenario(eventList, new TimeWindow(0, 1000), 1000L, SI
      // .MILLI(SI.SECOND), pc);
      // }
      // });

      final String res = ScenarioIO.write(s);
      assertEquals(s, ScenarioIO.read(res));
    }
  }

  enum TestProblemClass implements ProblemClass {
    TEST;

    @Override
    public String getId() {
      return name();
    }
  }

  // static class TestScenario extends PDPScenario {
  // final TimeWindow timeWindow;
  // final long tickSize;
  // final Unit<Duration> timeUnit;
  // final ProblemClass problemClass;
  //
  // public TestScenario(Collection<? extends TimedEvent> events, TimeWindow tw,
  // long ts, Unit<Duration> tu, ProblemClass pc) {
  // super(events, ImmutableSet.<Enum<?>> copyOf(PDPScenarioEvent.values()));
  // timeWindow = tw;
  // tickSize = ts;
  // timeUnit = tu;
  // problemClass = pc;
  // }
  //
  // @Override
  // public ImmutableList<? extends Supplier<? extends Model<?>>>
  // getModelSuppliers() {
  // throw new UnsupportedOperationException();
  // }
  //
  // @Override
  // public TimeWindow getTimeWindow() {
  // return timeWindow;
  // }
  //
  // @Override
  // public long getTickSize() {
  // return tickSize;
  // }
  //
  // @Override
  // public Predicate<Simulator> getStopCondition() {
  // throw new UnsupportedOperationException();
  // }
  //
  // @Override
  // public Unit<Duration> getTimeUnit() {
  // return timeUnit;
  // }
  //
  // @Override
  // public Unit<Velocity> getSpeedUnit() {
  // throw new UnsupportedOperationException();
  // }
  //
  // @Override
  // public Unit<Length> getDistanceUnit() {
  // throw new UnsupportedOperationException();
  // }
  //
  // @Override
  // public ProblemClass getProblemClass() {
  // return problemClass;
  // }
  //
  // @Override
  // public String getProblemInstanceId() {
  // throw new UnsupportedOperationException();
  // }
  // }
}
