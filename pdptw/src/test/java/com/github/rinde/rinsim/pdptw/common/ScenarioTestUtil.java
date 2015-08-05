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

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import java.math.RoundingMode;
import java.util.Collections;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioIO;
import com.github.rinde.rinsim.scenario.StopConditions;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.math.DoubleMath;

/**
 * Utilities for testing {@link Scenario}.
 * @author Rinde van Lon
 */
public class ScenarioTestUtil {

  private ScenarioTestUtil() {}

  /**
   * Tests whether the specified scenario can be correctly written to disk, it
   * compares the equality of the specified object with a parsed object and it
   * compares the equality of the serialized string.
   * @param input The scenario to test with IO.
   */
  public static void assertScenarioIO(Scenario input) {
    final String serialized = ScenarioIO.write(input);
    final Scenario parsed = ScenarioIO.read(serialized);
    final String serializedAgain = ScenarioIO.write(parsed);

    assertThat(input).isEqualTo(parsed);
    assertThat(serialized).isEqualTo(serializedAgain);
  }

  /**
   * Creates a random scenario.
   * @param seed The seed to use.
   * @param models Additional models (optional).
   * @return A new random scenario.
   */
  public static Scenario createRandomScenario(long seed,
      ModelBuilder<?, ?>... models) {
    final int endTime = 3 * 60 * 60 * 1000;
    final Scenario.Builder b = Scenario
        .builder()
        .addModel(RoadModelBuilders.plane())
        .addModel(DefaultPDPModel.builder())
        .addModels(asList(models))
        .addEvents(
          Collections
              .nCopies(
                10,
                AddVehicleEvent.create(-1, VehicleDTO
                    .builder()
                    .startPosition(new Point(5, 5))
                    .build())));

    final RandomGenerator rng = new MersenneTwister(seed);
    for (int i = 0; i < 20; i++) {
      final long announceTime = rng.nextInt(DoubleMath.roundToInt(
        endTime * .8, RoundingMode.FLOOR));
      b.addEvent(AddParcelEvent.create(Parcel
          .builder(
            new Point(rng.nextDouble() * 10, rng.nextDouble() * 10),
            new Point(rng.nextDouble() * 10, rng.nextDouble() * 10))
          .orderAnnounceTime(announceTime)
          .pickupTimeWindow(TimeWindow.create(announceTime, endTime))
          .deliveryTimeWindow(TimeWindow.create(announceTime, endTime))
          .neededCapacity(0).buildDTO()));
    }

    b.addEvent(TimeOutEvent.create(endTime))
        .scenarioLength(endTime)
        .setStopCondition(StopConditions.limitedTime(endTime));

    return b.build();
  }
}
