/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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

import javax.measure.unit.NonSI;

import org.junit.Test;

import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.scenario.IScenario;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.Scenario.ProblemClass;

/**
 * Tests IO of {@link PDPRoadModel}.
 * @author Rinde van Lon
 */
public class PDPRoadModelIOTest {
  /**
   * Tests that the {@link PDPRoadModel} supports serialization and
   * deserialization.
   */
  @Test
  public void testIO() {
    final Scenario.Builder sb = Scenario
      .builder(Scenario.DEFAULT_PROBLEM_CLASS)
      .addModel(PDPRoadModel.builder(
        RoadModelBuilders.plane()
          .withSpeedUnit(NonSI.MILES_PER_HOUR)
          .withMaxSpeed(7))
        .withAllowVehicleDiversion(true));

    final IScenario s = sb.problemClass(TestProblemClass.TEST).build();
    ScenarioTestUtil.assertScenarioIO(s);
  }

  enum TestProblemClass implements ProblemClass {
    TEST;

    @Override
    public String getId() {
      return name();
    }
  }
}
