/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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
package com.github.rinde.rinsim.pdptw.experiment;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.jppf.server.JPPFDriver;
import org.jppf.utils.JPPFConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem.StopConditions;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.common.TestObjectiveFunction;
import com.github.rinde.rinsim.pdptw.experiment.ExperimentTest.TestPostProcessor;
import com.github.rinde.rinsim.pdptw.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioTestUtil;
import com.github.rinde.rinsim.scenario.generator.Models;
import com.github.rinde.rinsim.scenario.generator.ScenarioGenerator;

/**
 * Test for JPPF computation.
 * @author Rinde van Lon 
 */
public class JppfTest {
  @SuppressWarnings("null")
  static JPPFDriver driver;
  @SuppressWarnings("null")
  static Scenario scenario;

  /**
   * Starts the JPPF driver.
   */
  @BeforeClass
  public static void setUp() {
    JPPFConfiguration.getProperties().setBoolean("jppf.local.node.enabled",
        true);
    JPPFDriver.main(new String[] { "noLauncher" });
    driver = JPPFDriver.getInstance();

    scenario = ScenarioTestUtil.create(123L);
  }

  /**
   * Stops the JPPF driver.
   */
  @AfterClass
  public static void tearDown() {
    driver.shutdown();
  }

  /**
   * Checks determinism of two subsequent identical JPPF experiments.
   */
  @Test
  public void determinismJppfVsJppf() {
    final List<Integer> ints = asList(1, 2, 5, 10);
    final List<ExperimentResults> allResults = newArrayList();

    final Experiment.Builder experimentBuilder = Experiment
        .build(TestObjectiveFunction.INSTANCE)
        .computeDistributed()
        .addScenario(scenario)
        .withRandomSeed(123)
        .repeat(10)
        .addConfiguration(TestMASConfiguration.create("A"));
    for (final int i : ints) {
      allResults.add(
          experimentBuilder.numBatches(i)
              .perform());
    }
    assertEquals(4, allResults.size());
    for (int i = 0; i < allResults.size() - 1; i++) {
      assertEquals(allResults.get(i), allResults.get(i + 1));
    }
  }

  /**
   * Checks determinism of a local experiment and a JPPF experiment, both with
   * identical settings. Using a Gendreau scenario.
   */
  @Test
  public void determinismLocalVsJppf() {
    final Experiment.Builder experimentBuilder = Experiment
        .build(TestObjectiveFunction.INSTANCE)
        .computeDistributed()
        .addScenario(scenario)
        .withRandomSeed(123)
        .repeat(1)
        .usePostProcessor(new TestPostProcessor())
        .addConfiguration(TestMASConfiguration.create("A"));

    final ExperimentResults results3 = experimentBuilder.perform();
    experimentBuilder.computeLocal();
    final ExperimentResults results4 = experimentBuilder.perform();
    assertEquals(results3, results4);
    assertTrue(results3.results.asList().get(0).simulationData.isPresent());
  }

  /**
   * Checks determinism of a local experiment and a JPPF experiment, both with
   * identical settings. Using a generated scenario.
   */
  @Test
  public void determinismGeneratedScenarioLocalVsJppf() {
    final RandomGenerator rng = new MersenneTwister(123L);
    final Scenario generatedScenario = ScenarioGenerator
        .builder()
        .addModel(Models.roadModel(20, true))
        .addModel(Models.pdpModel(TimeWindowPolicies.LIBERAL))
        .stopCondition(StopConditions.TIME_OUT_EVENT)
        .build().generate(rng, "hoi");

    final Experiment.Builder experimentBuilder = Experiment
        .build(Gendreau06ObjectiveFunction.instance())
        .computeDistributed()
        .addScenario(generatedScenario)
        .withRandomSeed(123)
        .repeat(1)
        .usePostProcessor(new TestPostProcessor())
        .addConfiguration(TestMASConfiguration.create("A"));

    final ExperimentResults resultsDistributed = experimentBuilder.perform();
    final ExperimentResults resultsLocal = experimentBuilder
        .computeLocal()
        .perform();
    assertEquals(resultsLocal, resultsDistributed);
  }

  /**
   * Tests a post processor that returns objects that does not implement
   * {@link Serializable}.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testFaultyPostProcessor() {
    Experiment.build(Gendreau06ObjectiveFunction.instance())
        .computeDistributed()
        .addScenario(scenario)
        .withRandomSeed(123)
        .repeat(1)
        .usePostProcessor(new TestFaultyPostProcessor())
        .addConfiguration(TestMASConfiguration.create("A"))
        .perform();

  }

  /**
   * Tests whether a not serializable objective function generates an exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testNotSerializableObjFunc() {
    Experiment
        .build(new NotSerializableObjFunc())
        .computeDistributed()
        .addScenario(scenario)
        .withRandomSeed(123)
        .repeat(1)
        .addConfiguration(TestMASConfiguration.create("A"))
        .perform();
  }

  static class TestFaultyPostProcessor implements
      PostProcessor<NotSerializable>, Serializable {
    private static final long serialVersionUID = -2166760289557525263L;

    @Override
    public NotSerializable collectResults(Simulator sim) {
      return new NotSerializable();
    }
  }

  static class NotSerializableObjFunc implements ObjectiveFunction {
    @Override
    public boolean isValidResult(StatisticsDTO stats) {
      return true;
    }

    @Override
    public double computeCost(StatisticsDTO stats) {
      return 0;
    }

    @Override
    public String printHumanReadableFormat(StatisticsDTO stats) {
      return "NotSerializableObjFunc";
    }
  }

  static class NotSerializable {}
}
