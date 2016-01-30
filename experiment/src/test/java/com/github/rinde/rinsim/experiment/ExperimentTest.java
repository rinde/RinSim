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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
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

  /**
   * Tests that that seed repetitions are correctly executed.
   */
  @Test
  public void testSeedRepetitions() {
    final Scenario s0 = ScenarioTestUtil.createRandomScenario(123L);
    final Scenario s1 = ScenarioTestUtil.createRandomScenario(456L);
    final MASConfiguration c0 = ExperimentTestUtil.testConfig("c0");
    final Experiment.Builder builder = Experiment
        .build(TestObjectiveFunction.INSTANCE)
        .addScenario(s0)
        .addScenario(s1)
        .addConfiguration(c0)
        .repeat(2)
        .withThreads(1)
        .repeatSeed(3)
        .withRandomSeed(123);

    final ExperimentResults er = builder.perform();
    final List<SimulationResult> results = new ArrayList<>(er.getResults());

    assertThat(results).hasSize(12);
    assertSimRes(results.get(0), s0, c0, 0);
    assertSimRes(results.get(1), s0, c0, 1);
    assertSimRes(results.get(2), s0, c0, 2);
    assertSeedEquality(results.subList(0, 3));
    assertSimRes(results.get(3), s0, c0, 0);
    assertSimRes(results.get(4), s0, c0, 1);
    assertSimRes(results.get(5), s0, c0, 2);
    assertSeedEquality(results.subList(3, 6));
    assertSimRes(results.get(6), s1, c0, 0);
    assertSimRes(results.get(7), s1, c0, 1);
    assertSimRes(results.get(8), s1, c0, 2);
    assertSeedEquality(results.subList(6, 9));
    assertSimRes(results.get(9), s1, c0, 0);
    assertSimRes(results.get(10), s1, c0, 1);
    assertSimRes(results.get(11), s1, c0, 2);
    assertSeedEquality(results.subList(9, 12));
  }

  @Test
  public void testOrdering() {
    final Scenario s0 = ScenarioTestUtil.createRandomScenario(123L);
    final Scenario s1 = ScenarioTestUtil.createRandomScenario(456L);
    final MASConfiguration c0 = ExperimentTestUtil.testConfig("c0");
    final MASConfiguration c1 = ExperimentTestUtil.testConfig("c1");
    final Experiment.Builder builder = Experiment
        .build(TestObjectiveFunction.INSTANCE)
        .addScenario(s0)
        .addScenario(s1)
        .addConfiguration(c0)
        .addConfiguration(c1)
        .repeat(2)
        .withThreads(1)
        .repeatSeed(2)
        .withOrdering(
          SimulationProperty.SEED_REPS,
          SimulationProperty.REPS,
          SimulationProperty.SCENARIO,
          SimulationProperty.CONFIG)
        .withRandomSeed(123);

    final ExperimentResults er = builder.perform();
    final List<SimulationResult> results = new ArrayList<>(er.getResults());

    assertThat(results).hasSize(16);
    assertSimRes(results.get(0), s0, c0, 0);
    assertSimRes(results.get(1), s0, c1, 0);
    assertSimRes(results.get(2), s1, c0, 0);
    assertSimRes(results.get(3), s1, c1, 0);
    assertSeedEquality(results.subList(0, 4));
    assertSimRes(results.get(4), s0, c0, 0);
    assertSimRes(results.get(5), s0, c1, 0);
    assertSimRes(results.get(6), s1, c0, 0);
    assertSimRes(results.get(7), s1, c1, 0);
    assertSeedEquality(results.subList(4, 8));
    assertSimRes(results.get(8), s0, c0, 1);
    assertSimRes(results.get(9), s0, c1, 1);
    assertSimRes(results.get(10), s1, c0, 1);
    assertSimRes(results.get(11), s1, c1, 1);
    assertSeedEquality(results.subList(8, 12));
    assertSimRes(results.get(12), s0, c0, 1);
    assertSimRes(results.get(13), s0, c1, 1);
    assertSimRes(results.get(14), s1, c0, 1);
    assertSimRes(results.get(15), s1, c1, 1);
    assertSeedEquality(results.subList(12, 16));
  }

  static void assertSimRes(SimulationResult sr, Scenario s, MASConfiguration c,
      int r) {
    assertThat(sr.getSimArgs().getScenario()).isEqualTo(s);
    assertThat(sr.getSimArgs().getMasConfig()).isEqualTo(c);
    assertThat(sr.getSimArgs().getRepetition()).isEqualTo(r);
  }

  static void assertSeedEquality(List<SimulationResult> results) {
    checkArgument(!results.isEmpty());
    final long seed = results.get(0).getSimArgs().getRandomSeed();
    for (final SimulationResult sr : results) {
      assertThat(sr.getSimArgs().getRandomSeed()).isEqualTo(seed);
    }
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
