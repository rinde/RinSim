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
package com.github.rinde.rinsim.examples;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.github.rinde.rinsim.examples.comm.CommExample;
import com.github.rinde.rinsim.examples.core.SimpleExample;
import com.github.rinde.rinsim.examples.core.taxi.TaxiExample;
import com.github.rinde.rinsim.examples.pdptw.gradientfield.GradientFieldExample;
import com.github.rinde.rinsim.examples.warehouse.WarehouseExample;
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
    WarehouseExample.run(true);
  }
}
