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
package com.github.rinde.rinsim.pdptw.common;

import static java.util.Arrays.asList;

import java.util.List;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.junit.Test;

import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.Scenario.ProblemClass;
import com.github.rinde.rinsim.scenario.Scenario.SimpleProblemClass;
import com.github.rinde.rinsim.scenario.StopConditions;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
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

    sb.addEvent(AddVehicleEvent.create(100,
      VehicleDTO.builder()
          .startPosition(new Point(7, 7))
          .speed(7d)
          .capacity(2)
          .availabilityTimeWindow(TimeWindow.create(0, 1000L))
          .build()))
        .addEvent(AddDepotEvent.create(76, new Point(3, 3)))
        .addEvent(AddVehicleEvent.create(125,
          VehicleDTO.builder()
              .startPosition(new Point(6, 9))
              .speed(3d)
              .capacity(1)
              .availabilityTimeWindow(TimeWindow.create(500, 10000L))
              .build()))
        .addEvent(AddParcelEvent.create(
          Parcel.builder(new Point(0, 0), new Point(1, 1))
              .pickupTimeWindow(TimeWindow.create(2500, 10000))
              .deliveryTimeWindow(TimeWindow.create(5000, 10000))
              .neededCapacity(0)
              .orderAnnounceTime(2400)
              .pickupDuration(200)
              .deliveryDuration(800)
              .buildDTO()))
        .addEvent(TimeOutEvent.create(200000))
        .addModel(RoadModelBuilders.plane()
            .withDistanceUnit(SI.CENTIMETER)
            .withMaxPoint(new Point(7d, 8.4))
            .withMinPoint(new Point(-1, 3))
            .withMaxSpeed(7d)
            .withSpeedUnit(NonSI.MILES_PER_HOUR))
        .addModel(DefaultPDPModel.builder()
            .withTimeWindowPolicy(TimeWindowPolicies.STRICT))
        .setStopCondition(
          StopConditions.and(StopConditions.alwaysTrue(),
            StopConditions.limitedTime(1000)));

    final List<ProblemClass> pcs = asList(TestProblemClass.TEST,
      SimpleProblemClass.create("hello"));
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
