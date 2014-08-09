package com.github.rinde.rinsim.scenario;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent;
import com.github.rinde.rinsim.core.pdptw.ParcelDTO;
import com.github.rinde.rinsim.core.pdptw.VehicleDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.AddDepotEvent;
import com.github.rinde.rinsim.scenario.AddParcelEvent;
import com.github.rinde.rinsim.scenario.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioIO;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.Scenario.ProblemClass;
import com.github.rinde.rinsim.scenario.Scenario.SimpleProblemClass;
import com.github.rinde.rinsim.util.TimeWindow;

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
      final Scenario s = sb.problemClass(pc).build();
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
}
