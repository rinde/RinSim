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
package com.github.rinde.rinsim.experiment.base;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jppf.server.JPPFDriver;
import org.jppf.utils.JPPFConfiguration;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.MockitoAnnotations;

import com.google.common.base.Function;

/**
 *
 * @author Rinde van Lon
 *
 */
public class ExperimentTest {

  @SuppressWarnings("null")
  static JPPFDriver driver;

  @SuppressWarnings("null")
  @Captor
  private ArgumentCaptor<SimResultContainer> captor;

  /**
   * Starts the JPPF driver.
   */
  @BeforeClass
  public static void setUp() {
    JPPFConfiguration.getProperties().setBoolean("jppf.local.node.enabled",
        true);
    JPPFDriver.main(new String[] { "noLauncher" });
    driver = JPPFDriver.getInstance();
  }

  /**
   * Stops the JPPF driver.
   */
  @AfterClass
  public static void tearDown() {
    driver.shutdown();
  }

  /**
   * 
   */
  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
  }

  /**
   * Tests usage via inheritance.
   */
  @Test
  public void testInheritance() {
    TestExperiment te = new TestExperiment();
    te.addConfiguration(new TestConfig("config1"))
        .addConfiguration(new TestConfig("config2"))
        .addScenario(
            new DefaultScenario(new StringProblemClass("class0"), "instance0"))
        .repeat(5);

    ExperimentResults er = te.perform();
    assertEquals(10, er.sortedResults().size());
  }

  /**
   * Tests usage via default builder.
   */
  @Test
  public void testDefaultBuilder() {
    ExperimentResults er = ExperimentBuilder
        .defaultInstance(ComputeFunc.INSTANCE)
        .addConfiguration(new TestConfig("config1"))
        .addConfiguration(new TestConfig("config2"))
        .addConfiguration(new TestConfig("config3"))
        .addScenario(
            new DefaultScenario(new StringProblemClass("class0"), "instance0"))
        .addScenario(
            new DefaultScenario(new StringProblemClass("class0"), "instance1"))
        .repeat(10)
        .perform();

    assertEquals(60, er.results.size());
  }

  /**
   * Test for checking that {@link ResultListener} is called in correct order
   * and correct number of times.
   */
  @Test
  public void testResultListener() {
    ResultListener list = mock(ResultListener.class);

    // setup: 3 x 2 x 10 = 60 simulations
    ExperimentResults er = ExperimentBuilder
        .defaultInstance(ComputeFunc.INSTANCE)
        .addConfiguration(new TestConfig("config1"))
        .addConfiguration(new TestConfig("config2"))
        .addConfiguration(new TestConfig("config3"))
        .addScenario(
            new DefaultScenario(new StringProblemClass("class0"), "instance0"))
        .addScenario(
            new DefaultScenario(new StringProblemClass("class0"), "instance1"))
        .repeat(10)
        .addResultListener(list)
        .perform();

    InOrder inOrder = inOrder(list);
    inOrder.verify(list).startComputing(60);
    inOrder.verify(list, times(60)).receive(captor.capture());
    inOrder.verify(list, times(1)).doneComputing();
    assertEquals(60, er.results.size());
  }

  @Test
  public void testJppf() {

    ExperimentBuilder.defaultInstance(ComputeFunc.INSTANCE)
        .addConfiguration(new TestConfig("config1"))
        .addScenario(
            new DefaultScenario(new StringProblemClass("class0"), "instance0"))
        .repeat(10)
        .computeDistributed()
        .perform();
  }

  enum ComputeFunc implements Function<SimArgs, SimResult> {
    INSTANCE {
      @Override
      @Nonnull
      public SimResult apply(@Nullable SimArgs input) {
        return new Result(requireNonNull(input));
      }
    }
  }

  static class TestExperiment extends ExperimentBuilder<TestExperiment> {
    TestExperiment() {
      super(ComputeFunc.INSTANCE);
    }

    @Override
    protected TestExperiment self() {
      return this;
    }

  }

  static class Result implements SimResult, Serializable {
    private static final long serialVersionUID = 8819637183171640895L;
    final String args;

    Result(SimArgs args) {
      this.args = args.toString();
    }

    @Override
    public String toString() {
      return toStringHelper("Result").add("args", args).toString();
    }
  }

  static class TestConfig implements Configuration, Serializable {
    private final String name;

    TestConfig(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }

    @Override
    public int compareTo(@Nullable Configuration o) {
      return name.compareTo(requireNonNull(o).toString());
    }
  }
}
