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
package com.github.rinde.rinsim.examples;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.github.rinde.rinsim.examples.agv.AgvExample;
import com.github.rinde.rinsim.examples.comm.CommExample;
import com.github.rinde.rinsim.examples.experiment.ExperimentExample;
import com.github.rinde.rinsim.examples.gradientfield.GradientFieldExample;
import com.github.rinde.rinsim.examples.simple.SimpleExample;
import com.github.rinde.rinsim.examples.taxi.TaxiExample;
import com.github.rinde.rinsim.examples.uav.UavExample;
import com.github.rinde.rinsim.testutil.GuiTests;

/**
 * Tests whether all examples run.
 * @author Rinde van Lon
 */
@Category(GuiTests.class)
public class ExamplesTest {

  /**
   * Tests the taxi example.
   */
  @Test
  public void taxiExample() {
    TaxiExample.run(true);
  }

  /**
   * Tests the simple example.
   */
  @Test
  public void simpleExample() {
    SimpleExample.run(true);
  }

  /**
   * Tests the communication example.
   */
  @Test
  public void communicationExample() {
    CommExample.run(true);
  }

  /**
   * Run the gradient field example class.
   */
  @Test
  public void gradientFieldExample() {
    try {
      GradientFieldExample.run(true);
    } catch (final RuntimeException e) {
      // find the root cause of the exception
      Throwable cur = e;
      while (cur.getCause() != null) {
        cur = cur.getCause();
      }
      // verify that the exception was caused by the early termination of the
      // simulation
      assertTrue(cur.toString(), cur instanceof IllegalStateException);
      assertThat(cur.getMessage()).containsMatch(
        "The simulation did not result in a valid result");
    }
  }

  /**
   * Runs the warehouse example.
   */
  @Test
  public void warehouseExample() {
    AgvExample.run(true);
  }

  /**
   * Runs the experiment example.
   */
  @Test
  public void experimentExample() {
    ExperimentExample.main(new String[] {"speedup", "64"});
  }

  /**
   * Runs the UAV example.
   */
  @Test
  public void uavExample() {
    UavExample.run(true);
  }
}
