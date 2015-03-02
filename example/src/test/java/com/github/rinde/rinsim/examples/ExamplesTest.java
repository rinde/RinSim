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
package com.github.rinde.rinsim.examples;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.github.rinde.rinsim.examples.core.SimpleExample;
import com.github.rinde.rinsim.examples.core.comm.AgentCommunicationExample;
import com.github.rinde.rinsim.examples.core.taxi.TaxiExample;
import com.github.rinde.rinsim.examples.pdptw.gradientfield.GradientFieldExample;
import com.github.rinde.rinsim.testutil.GuiTests;

@Category(GuiTests.class)
public class ExamplesTest {

  @Test
  public void taxiExample() {
    TaxiExample.run(true);
  }

  @Test
  public void simpleExample() {
    SimpleExample.run(true);
  }

  @Test
  public void communicationExample() throws Exception {
    AgentCommunicationExample.run(true);
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
      assertTrue(cur.getMessage().contains(
          "The simulation did not result in a valid result"));
    }
  }
}
