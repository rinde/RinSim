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
package com.github.rinde.rinsim.central.rt;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.github.rinde.rinsim.central.RandomSolver;
import com.github.rinde.rinsim.core.model.time.RealTimeClockController.ClockMode;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Parser;

/**
 * Test for {@link RtCentral}.
 * @author Rinde van Lon
 */
public class RtCentralTest {

  /**
   * Tests that the configuration works and always produces the same result.
   */
  @Test
  public void testConfig() {
    List<TimedEvent> events = Gendreau06Parser.parse(
        new File("../scenario-util/files/test/gendreau06/req_rapide_1_240_24"))
        .getEvents().subList(0, 20);

    Scenario s = Scenario.builder(Gendreau06Parser.parse(
        new File("../scenario-util/files/test/gendreau06/req_rapide_1_240_24")))
        .removeModelsOfType(TimeModel.AbstractBuilder.class)
        .addModel(TimeModel.builder()
            .withRealTime()
            .withStartInClockMode(ClockMode.SIMULATED))
        .clearEvents()
        .addEvents(events)
        .addEvent(TimeOutEvent.create(3 * 60 * 60 * 1000))
        .build();

    ExperimentResults er = Experiment
        .build(Gendreau06ObjectiveFunction.instance())
        .addScenario(s)
        .withThreads(1)
        .addConfiguration(
            RtCentral.solverConfigurationAdapt(RandomSolver.supplier(), ""))
        .perform();

    double objVal = Gendreau06ObjectiveFunction.instance()
        .computeCost(er.results.asList().get(0).stats);
    assertThat(objVal).isWithin(0.0001).of(495.4718);
  }
}
