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

import static com.github.rinde.rinsim.cli.CliTest.testFail;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.cli.CliException.CauseType;
import com.github.rinde.rinsim.cli.Menu;
import com.github.rinde.rinsim.experiment.Experiment.Builder;
import com.github.rinde.rinsim.pdptw.common.ScenarioTestUtil;
import com.github.rinde.rinsim.pdptw.common.TestObjectiveFunction;
import com.github.rinde.rinsim.scenario.ScenarioIO;
import com.github.rinde.rinsim.testutil.TestUtil;

/**
 * Test for commandline interface of experiment.
 * @author Rinde van Lon
 */
public class ExperimentCliTest {

  @SuppressWarnings("null")
  MASConfiguration configA;
  @SuppressWarnings("null")
  MASConfiguration configB;
  @SuppressWarnings("null")
  MASConfiguration configC;
  @SuppressWarnings("null")
  Builder builder;
  @SuppressWarnings("null")
  Menu menu;

  /**
   * Set up the experiment builder and create the CLI menu.
   */
  @Before
  public void setUp() {
    TestUtil.testEnum(ExperimentCli.ConfigToName.class);
    configA = ExperimentTestUtil.testConfig("A");
    configB = ExperimentTestUtil.testConfig("B");
    configC = ExperimentTestUtil.testConfig("C");
    builder = Experiment
      .build(TestObjectiveFunction.INSTANCE)
      .addScenario(ScenarioTestUtil.createRandomScenario(1655L))
      .addConfiguration(configA)
      .addConfiguration(configB)
      .addConfiguration(configC)
      .setScenarioReader(ScenarioIO.reader());

    menu = ExperimentCli.createMenu(builder);
  }

  Builder testSuccess(String args) {
    ExperimentCli.execute(builder, args.split(" "));
    return builder;
  }

  /**
   * Test for various inputs of seed.
   */
  @Test
  public void testSeed() {
    testFail(menu, "s", CauseType.MISSING_ARG, "-s");
    testFail(menu, "s", CauseType.INVALID_ARG_FORMAT, "-b", "10", "-s", "x");
    assertEquals(10, testSuccess("-s 10").masterSeed);
  }

  /**
   * Test for various inputs of batches.
   */
  @Test
  public void testBatches() {
    testFail(menu, "b", CauseType.MISSING_ARG, "--batches");
    testFail(menu, "b", CauseType.INVALID_ARG_FORMAT, "-b", "x", "-s", "1");
    testFail(menu, "b", CauseType.HANDLER_FAILURE,
      IllegalArgumentException.class,
      "-b", "0", "-s", "1");
    assertEquals(1, testSuccess("-b 1").numBatches);
    assertEquals(10, testSuccess("-b 10").numBatches);
  }

  /**
   * Test for various inputs of threads.
   */
  @Test
  public void testThreads() {
    testFail(menu, "t", CauseType.MISSING_ARG, "--threads");
    testFail(menu, "t", CauseType.INVALID_ARG_FORMAT, "-t", "x", "-s", "1");
    testFail(menu, "t", CauseType.HANDLER_FAILURE,
      IllegalArgumentException.class,
      "-t", "0", "-s", "1");
    assertEquals(1, testSuccess("-t 1").numThreads);
    assertEquals(10, testSuccess("-t 10").numThreads);
  }

  /**
   * Test for various inputs of repetitions.
   */
  @Test
  public void testRepetitions() {
    testFail(menu, "r", CauseType.MISSING_ARG, "--repetitions");
    testFail(menu, "r", CauseType.INVALID_ARG_FORMAT, "-r", "x", "-s", "1");
    testFail(menu, "r", CauseType.HANDLER_FAILURE,
      IllegalArgumentException.class,
      "-r", "0", "-s", "1");
    assertEquals(1, testSuccess("-r 1").repetitions);
    assertEquals(10, testSuccess("-r 10").repetitions);
  }

  /**
   * Test for various inputs of repetitions.
   */
  @Test
  public void testSeedRepetitions() {
    testFail(menu, "sr", CauseType.MISSING_ARG, "--seed-repetitions");
    testFail(menu, "sr", CauseType.INVALID_ARG_FORMAT, "-sr", "x", "-s", "1");
    testFail(menu, "sr", CauseType.HANDLER_FAILURE,
      IllegalArgumentException.class,
      "-sr", "0", "-s", "1");
    assertEquals(1, testSuccess("-sr 1").seedRepetitions);
    assertEquals(10, testSuccess("-sr 10").seedRepetitions);
  }

  /**
   * Tests whether the include option is effective.
   */
  @Test
  public void testInclude() {
    testFail(menu, "i", CauseType.MISSING_ARG, "--include");
    testFail(menu, "i", CauseType.INVALID_ARG_FORMAT,
      "--include", "x2");
    testFail(menu, "i", CauseType.INVALID_ARG_FORMAT,
      "--include", "c1,,c2");
    testFail(menu, "i", CauseType.HANDLER_FAILURE,
      IllegalArgumentException.class,
      "--i", "c1,c1,c1,c1");
    setUp();

    assertEquals(newHashSet(configB, configC),
      testSuccess("-i c1,c2").configurationsSet);
    setUp();

    assertEquals(newHashSet(configA),
      testSuccess("-i c0").configurationsSet);
    setUp();

    assertEquals(newHashSet(configA, configB, configC),
      testSuccess("-i c0,c2,c1").configurationsSet);
    setUp();

    assertEquals(newHashSet(configC),
      testSuccess("-i c2,c2,c2").configurationsSet);
  }

  /**
   * Tests whether the exclude option is effective.
   */
  @Test
  public void testExclude() {
    testFail(menu, "e", CauseType.MISSING_ARG, "--exclude");
    testFail(menu, "e", CauseType.INVALID_ARG_FORMAT,
      "--exclude", "x2");
    testFail(menu, "e", CauseType.INVALID_ARG_FORMAT,
      "--exclude", "c1,,c2");
    testFail(menu, "e", CauseType.HANDLER_FAILURE,
      IllegalArgumentException.class,
      "--e", "c1,c1,c1,c1");
    testFail(menu, "e", CauseType.INVALID_ARG_FORMAT,
      "--e", "c0,c1,c");
    setUp();

    assertEquals(newHashSet(configA),
      testSuccess("-e c1,c2").configurationsSet);
    setUp();

    assertEquals(newHashSet(configB, configC),
      testSuccess("-e c0").configurationsSet);
    setUp();

    assertEquals(newHashSet(configA),
      testSuccess("-e c2,c1").configurationsSet);
    setUp();

    assertEquals(newHashSet(configA, configB),
      testSuccess("-e c2,c2").configurationsSet);
  }

  /**
   * Batches and threads can not be used together.
   */
  @Test
  public void batchesThreadsFail() {
    testFail(menu, "t", CauseType.ALREADY_SELECTED, "--batches", "2", "-t",
      "4");
    testFail(menu, "b", CauseType.ALREADY_SELECTED, "--threads", "4", "--b",
      "2");
  }

  /**
   * Include and exclude can not be used together.
   */
  @Test
  public void excludesIncludesFail() {
    testFail(menu, "i", CauseType.ALREADY_SELECTED, "-e", "c1", "-i", "c1");
    testFail(menu, "e", CauseType.ALREADY_SELECTED, "-i", "c1", "-e", "c1");
  }

  /**
   * Jppf and local can not be used together.
   */
  @Test
  public void localJppfFail() {
    testFail(menu, "j", CauseType.ALREADY_SELECTED, "--local", "--jppf");
    testFail(menu, "l", CauseType.ALREADY_SELECTED, "--jppf", "--local");
  }

  /**
   * Tests help.
   */
  @Test
  public void testHelp() {
    testSuccess("-h");
    builder.perform(System.out, "-h");
  }

  /**
   * Tests a dry run of an experiment.
   */
  @Test
  public void dryRun() {
    builder.perform(System.out, "-dr", "v");
  }

  /**
   * Test correct applying of order.
   */
  @Test
  public void testOrdering() {
    testFail(menu, "o", CauseType.HANDLER_FAILURE, "-o", "CONFIG,SCENARIO");
    testFail(menu, "o", CauseType.HANDLER_FAILURE, "-o",
      "SEED_REPS,REPS,SCENARIO,CONFIG,SEED_REPS");

    assertThat(builder.experimentOrdering)
      .isEqualTo(Experiment.Builder.DEFAULT_EXPERIMENT_ORDERING);

    menu.execute("-o", "SEED_REPS,REPS,SCENARIO,CONFIG");

    assertThat(builder.experimentOrdering)
      .containsExactly(
        SimulationProperty.SEED_REPS,
        SimulationProperty.REPS,
        SimulationProperty.SCENARIO,
        SimulationProperty.CONFIG)
      .inOrder();
  }

  /**
   * Test the warmup option.
   */
  @Test
  public void testWarmup() {
    assertThat(menu.execute("-w", "123")).isAbsent();
    assertThat(builder.warmupPeriodMs).isEqualTo(123);
    testFail(menu, "w", CauseType.HANDLER_FAILURE, "-w", "-9");
  }
}
