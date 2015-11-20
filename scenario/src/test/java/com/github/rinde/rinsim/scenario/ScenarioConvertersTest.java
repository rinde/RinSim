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
package com.github.rinde.rinsim.scenario;

import static com.google.common.base.Verify.verifyNotNull;
import static com.google.common.truth.Truth.assertThat;

import javax.measure.unit.NonSI;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.testutil.TestUtil;

/**
 * Tests for {@link ScenarioConverters}.
 * @author Rinde van Lon
 */
public class ScenarioConvertersTest {
  /**
   * Tests some unreachable code.
   */
  @BeforeClass
  public static void setUpClass() {
    TestUtil.testPrivateConstructor(ScenarioConverters.class);
    TestUtil.testEnum(ScenarioConverters.TimeModelConverter.class);
  }

  /**
   * Tests that when no time model is available, a default is added.
   */
  @Test
  public void testEmpty() {
    final Scenario empty = Scenario.builder().build();
    assertThat(empty.getModelBuilders()).isEmpty();

    final Scenario convertedEmpty =
        verifyNotNull(ScenarioConverters.toRealtime().apply(empty));
    assertThat(convertedEmpty.getModelBuilders())
        .contains(TimeModel.builder()
            .withRealTime()
            .withStartInClockMode(ClockMode.SIMULATED));
  }

  /**
   * Tests that when a time model already exists, its properties are copied.
   */
  @Test
  public void testCopyProperties() {
    final Scenario s = Scenario.builder()
        .addModel(TimeModel.builder()
            .withTickLength(754L)
            .withTimeUnit(NonSI.DAY))
        .addModel(CommModel.builder())
        .build();

    final Scenario converted =
        verifyNotNull(ScenarioConverters.toRealtime().apply(s));
    assertThat(converted.getModelBuilders())
        .contains(TimeModel.builder()
            .withRealTime()
            .withStartInClockMode(ClockMode.SIMULATED)
            .withTickLength(754L)
            .withTimeUnit(NonSI.DAY));
  }

  /**
   * Tests that IAE is thrown when there are too many time models.
   */
  @Test
  public void testTooManyTimeModels() {
    final Scenario s = Scenario.builder()
        .addModel(TimeModel.builder())
        .addModel(TimeModel.builder().withRealTime())
        .build();
    boolean fail = false;
    try {
      ScenarioConverters.toRealtime().apply(s);
    } catch (final IllegalArgumentException e) {
      fail = true;
      assertThat(e.getMessage())
          .isEqualTo("More than one time model is not supported.");
    }
    assertThat(fail).isTrue();
  }
}
