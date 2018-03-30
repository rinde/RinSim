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
package com.github.rinde.rinsim.core.model.time;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import java.util.Collection;

import javax.measure.unit.NonSI;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import com.github.rinde.rinsim.core.model.FakeDependencyProvider;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.RtClockEventType;
import com.github.rinde.rinsim.core.model.time.TimeModel.AbstractBuilder;
import com.github.rinde.rinsim.core.model.time.TimeModel.RealtimeBuilder;
import com.github.rinde.rinsim.testutil.TestUtil;

/**
 * @author Rinde van Lon
 *
 */
public class RealtimeModelTest extends TimeModelTest<RealtimeModel> {

  /**
   * @param sup The supplier to use for creating model instances.
   */
  public RealtimeModelTest(AbstractBuilder<?> sup) {
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
   * Test unreachable code of enums.
   */
  @Test
  public void testEnums() {
    TestUtil.testEnum(ClockMode.class);
    TestUtil.testEnum(RtClockEventType.class);
  }

  /**
   * Tests that restarting the time is forbidden.
   */
  @Test
  public void testStartStopStart() {
    final LimitingTickListener ltl = new LimitingTickListener(getModel(), 3);
    getModel().register(ltl);
    getModel().start();
    boolean fail = false;
    try {
      getModel().start();
    } catch (final IllegalStateException e) {
      fail = true;
      assertThat(e.getMessage()).contains("can be started only once");
    }
    assertThat(fail).isTrue();
    assertThat(getModel().isExecutorAlive()).isFalse();
  }

  /**
   * Tests that calling tick is unsupported.
   */
  @SuppressWarnings("deprecation")
  @Test
  public void testTick() {
    boolean fail = false;
    try {
      getModel().tick();
    } catch (final UnsupportedOperationException e) {
      fail = true;
      assertThat(e.getMessage()).contains("not supported");
    }
    assertThat(fail).isTrue();
    assertThat(getModel().isExecutorAlive()).isFalse();
  }

  /**
   * Tests that clock mode is correctly set through the builder.
   */
  @Test
  public void testBuilderClockMode() {
    final RealtimeModel tm1 = (RealtimeModel) TimeModel.builder()
      .withRealTime()
      .build(FakeDependencyProvider.empty());
    assertThat(tm1.getClockMode()).isEqualTo(ClockMode.REAL_TIME);

    final RealtimeModel tm2 = (RealtimeModel) TimeModel.builder()
      .withRealTime()
      .withStartInClockMode(ClockMode.SIMULATED)
      .withTimeUnit(NonSI.HOUR)
      .withTickLength(1)
      .build(FakeDependencyProvider.empty());

    assertThat(tm2.getClockMode()).isEqualTo(ClockMode.SIMULATED);
    assertThat(tm2.getTimeUnit()).isEqualTo(NonSI.HOUR);
    assertThat(tm2.getTickLength()).isEqualTo(1);

    boolean fail = false;
    try {
      @SuppressWarnings("unused")
      final RealtimeBuilder b = TimeModel.builder()
        .withRealTime()
        .withStartInClockMode(ClockMode.STOPPED);
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("Can not use");
      fail = true;
    }
    assertThat(fail).isTrue();
    assertThat(getModel().isExecutorAlive()).isFalse();
  }

  /**
   * Tests that the model provides the correct objects.
   */
  @Test
  public void testProvidingTypes() {
    assertThat(getModel().get(Clock.class)).isNotNull();
    assertThat(getModel().get(ClockController.class)).isNotNull();
    assertThat(getModel().get(RealtimeClockController.class)).isNotNull();
    boolean fail = false;
    try {
      getModel().get(Object.class);
    } catch (final IllegalArgumentException e) {
      fail = true;
      assertThat(e.getMessage())
        .contains("does not provide instances of java.lang.Object");
    }
    assertThat(fail).isTrue();
    assertThat(getModel().isExecutorAlive()).isFalse();
  }

}
