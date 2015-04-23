/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.scenario;

import static java.util.Arrays.asList;

import java.util.List;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.junit.Test;

import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.pdptw.ParcelDTO;
import com.github.rinde.rinsim.core.pdptw.VehicleDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.Scenario.ProblemClass;
import com.github.rinde.rinsim.scenario.Scenario.SimpleProblemClass;
import com.github.rinde.rinsim.util.TimeWindow;

/**
 * Scenario IO test.
 * @author Rinde van Lon
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

    sb.addEvent(new AddVehicleEvent(100,
      VehicleDTO.builder()
        .startPosition(new Point(7, 7))
        .speed(7d)
        .capacity(2)
        .availabilityTimeWindow(new TimeWindow(0, 1000L))
        .build())
      )
      .addEvent(new AddDepotEvent(76, new Point(3, 3)))
      .addEvent(new AddVehicleEvent(125,
        VehicleDTO.builder()
          .startPosition(new Point(6, 9))
          .speed(3d)
          .capacity(1)
          .availabilityTimeWindow(new TimeWindow(500, 10000L))
          .build())
      )
      .addEvent(new AddParcelEvent(
        ParcelDTO.builder(new Point(0, 0), new Point(1, 1))
          .pickupTimeWindow(new TimeWindow(2500, 10000))
          .deliveryTimeWindow(new TimeWindow(5000, 10000))
          .neededCapacity(0)
          .orderAnnounceTime(2400)
          .pickupDuration(200)
          .deliveryDuration(800)
          .build())
      )
      .addEvent(new TimedEvent(PDPScenarioEvent.TIME_OUT, 200000))
      .addModel(RoadModelBuilders.plane()
        .setDistanceUnit(SI.CENTIMETER)
        .setMaxPoint(new Point(7d, 8.4))
        .setMinPoint(new Point(-1, 3))
        .setMaxSpeed(7d)
        .setSpeedUnit(NonSI.MILES_PER_HOUR)
      )
      .addModel(DefaultPDPModel.builder()
        .setTimeWindowPolicy(TimeWindowPolicies.STRICT)
      );

    final List<ProblemClass> pcs = asList(TestProblemClass.TEST,
      new SimpleProblemClass("hello"));
    for (final ProblemClass pc : pcs) {
      ScenarioTestUtil.assertScenarioIO(sb.problemClass(pc).build());
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
