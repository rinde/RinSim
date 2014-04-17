package rinde.sim.pdptw.vanlon14;

import static junit.framework.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.pdptw.common.AddDepotEvent;
import rinde.sim.pdptw.common.AddParcelEvent;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.ParcelDTO;
import rinde.sim.pdptw.common.VehicleDTO;
import rinde.sim.pdptw.scenario.ScenarioIO;
import rinde.sim.pdptw.scenario.PDPScenario.ProblemClass;
import rinde.sim.scenario.ScenarioBuilder;
import rinde.sim.scenario.ScenarioBuilder.ScenarioCreator;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

public class VanLon14ScenarioTest {

  @Ignore
  @Test
  public void test() {
    // FIXME should work
    // VanLon14.generateDataSet(new File("files/dataset/"));
  }

  @Test
  public void testIO() {
    final ScenarioBuilder sb = new ScenarioBuilder(PDPScenarioEvent.ADD_DEPOT,
        PDPScenarioEvent.ADD_PARCEL, PDPScenarioEvent.ADD_VEHICLE,
        PDPScenarioEvent.TIME_OUT);

    sb.addEvent(new AddVehicleEvent(100, new VehicleDTO(new Point(7, 7), 7d, 2,
        new TimeWindow(0, 1000L))));
    sb.addEvent(new AddDepotEvent(76, new Point(3, 3)));
    sb.addEvent(new AddVehicleEvent(125, new VehicleDTO(new Point(6, 9), 3d, 1,
        new TimeWindow(500, 10000L))));
    sb.addEvent(new AddParcelEvent(ParcelDTO
        .builder(new Point(0, 0), new Point(1, 1))
        .pickupTimeWindow(new TimeWindow(2500, 10000))
        .deliveryTimeWindow(new TimeWindow(5000, 10000))
        .neededCapacity(0)
        .arrivalTime(2400)
        .pickupDuration(200)
        .deliveryDuration(800)
        .build()));
    sb.addEvent(new TimedEvent(PDPScenarioEvent.TIME_OUT, 200000));

    final VanLon14Scenario s = sb
        .build(new ScenarioCreator<VanLon14Scenario>() {
          @Override
          public VanLon14Scenario create(List<TimedEvent> eventList,
              Set<Enum<?>> eventTypes) {
            return new VanLon14Scenario(eventList, new TimeWindow(0, 10),
                new Point(0, 0), new Point(10, 10), 1000L,
                VanLon14.ExperimentClass.HIGH_LARGE, 0);
          }
        });

    final String output = ScenarioIO.write(s);
    final VanLon14Scenario converted = ScenarioIO.read(output,
        VanLon14Scenario.class);
    assertEquals(s, converted);
  }

  enum PC implements ProblemClass {
    TEST;

    @Override
    public String getId() {
      return "TEST";
    }
  }
}
