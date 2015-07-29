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
package com.github.rinde.rinsim.core.model.time;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import java.util.Collection;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.github.rinde.rinsim.core.model.time.TimeModel.AbstractBuilder;
import com.github.rinde.rinsim.testutil.RealtimeTests;

/**
 * Tests for real-time model that are time sensitive (hence the category).
 * @author Rinde van Lon
 */
@Category(RealtimeTests.class)
public class RealtimeModelTestRT extends TimeModelTest<RealtimeModel> {

  /**
   * @param sup The supplier to use for creating model instances.
   */
  public RealtimeModelTestRT(AbstractBuilder<?> sup) {
    super(sup);
  }

  /**
   * @return The models to test.
   */
  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {TimeModel.builder().withRealTime().withTickLength(100L)}
    });
  }

  /**
   * Tests the actual elapsed time.
   */
  @Test
  public void testRealTime() {
    getModel().register(new LimitingTickListener(getModel(), 3));
    final long start = System.nanoTime();
    getModel().start();
    final long duration = System.nanoTime() - start;
    // duration should be somewhere between 200 and 300 ms
    assertThat(duration).isAtLeast(200000000L);
    assertThat(duration).isLessThan(300000000L);
    assertThat(getModel().getCurrentTime()).isEqualTo(300);
  }

}
