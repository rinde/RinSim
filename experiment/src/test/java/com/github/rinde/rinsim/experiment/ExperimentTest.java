/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.experiment;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.ScenarioTestUtil;
import com.github.rinde.rinsim.pdptw.common.StatsTracker;
import com.github.rinde.rinsim.pdptw.common.TestObjectiveFunction;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.testutil.TestUtil;
import com.google.auto.value.AutoValue;

/**
 * @author Rinde van Lon
 *
 */
public class ExperimentTest {

  @Test
  public void testCustomPostProcessor() {
    TestUtil.testEnum(Experiment.Computers.class);

    final Scenario scenario = ScenarioTestUtil.createRandomScenario(123L,
        StatsTracker.builder());
    final Experiment.Builder builder = Experiment
        .build(TestObjectiveFunction.INSTANCE)
        .addScenario(scenario)
        .addConfiguration(ExperimentTestUtil.testConfig("test"))
        .usePostProcessor(ExperimentTestUtil.testPostProcessor())
        .withRandomSeed(123);

    final ExperimentResults er = builder.perform();
    assertEquals(123, er.getMasterSeed());
    assertEquals(123,
        er.getResults().asList().get(0).getSimArgs().getRandomSeed());

    @SuppressWarnings("unchecked")
    final List<Point> positions =
        (List<Point>) er.getResults().asList().get(0).getResultObject();
    assertEquals(10, positions.size());
  }

  /**
   * Tests default processor.
   */
  @Test
  public void testDefaultPostProcessor() {
    final Scenario scenario = ScenarioTestUtil.createRandomScenario(123L,
        StatsTracker.builder());
    final Experiment.Builder builder = Experiment
        .build(TestObjectiveFunction.INSTANCE)
        .addScenario(scenario)
        .addConfiguration(ExperimentTestUtil.testConfig("test"))
        .withRandomSeed(123);

    final ExperimentResults er = builder.perform();

    assertThat(er.getResults().asList().get(0).getResultObject())
        .isInstanceOf(String.class);
    assertThat(er.getResults().asList().get(0).getResultObject())
        .isEqualTo("simulation duration: 10801000");
  }

  /**
   * Test correct handling of failure during simulation.
   */
  @Test
  public void testDefaultPostProcessorFailure() {
    final Scenario scenario = ScenarioTestUtil.createRandomScenario(123L,
        StatsTracker.builder(), FailureModel.builder());
    final Experiment.Builder builder = Experiment
        .build(TestObjectiveFunction.INSTANCE)
        .addScenario(scenario)
        .addConfiguration(ExperimentTestUtil.testConfig("test"))
        .withThreads(1)
        .repeat(3)
        .withRandomSeed(123);

    boolean fail = false;
    try {
      builder.perform();
    } catch (final AbortExperimentException e) {
      assertThat(e.getCause().getMessage()).contains("FailureModel");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  @Test
  public void testRetryPostProcessor() {
    final Scenario scenario = ScenarioTestUtil.createRandomScenario(123L,
        StatsTracker.builder());
    final Experiment.Builder builder = Experiment
        .build(TestObjectiveFunction.INSTANCE)
        .addScenario(scenario)
        .addConfiguration(ExperimentTestUtil.testConfig("test"))
        .withThreads(1)
        .usePostProcessor(ExperimentTestUtil.retryOncePostProcessor())
        .repeat(3)
        .withRandomSeed(123);

    builder.perform();
  }

  static class FailureModel extends AbstractModelVoid implements TickListener {

    FailureModel() {}

    @Override
    public void tick(TimeLapse timeLapse) {
      if (timeLapse.getStartTime() == 5000) {
        throw new IllegalStateException(
            "Hello! Yes? This is the FailureModel speaking.");
      }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

    static ModelBuilder<?, ?> builder() {
      return new AutoValue_ExperimentTest_FailureModel_FailureModelBuilder();
    }

    @AutoValue
    static class FailureModelBuilder
        extends AbstractModelBuilder<FailureModel, Void> {

      @Override
      public FailureModel build(DependencyProvider dependencyProvider) {
        return new FailureModel();
      }
    }

  }
}
