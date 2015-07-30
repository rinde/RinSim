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

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.measure.unit.NonSI;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import com.github.rinde.rinsim.core.model.FakeDependencyProvider;
import com.github.rinde.rinsim.core.model.time.Clock.ClockEventType;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.RtClockEventType;
import com.github.rinde.rinsim.core.model.time.TimeModel.AbstractBuilder;
import com.github.rinde.rinsim.core.model.time.TimeModel.RealtimeBuilder;
import com.github.rinde.rinsim.event.ListenerEventHistory;
import com.github.rinde.rinsim.testutil.TestUtil;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.math.DoubleMath;

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
  }

  /**
   * Tests that a sudden delay in computation time is detected.
   */
  @Test
  public void testConsistencyCheck() {
    getModel().register(limiter(150));

    final int t = RealtimeModel.Realtime.CONSISTENCY_CHECK_LENGTH + DoubleMath
        .roundToInt(.5 * RealtimeModel.Realtime.CONSISTENCY_CHECK_LENGTH,
            RoundingMode.HALF_DOWN);

    getModel().register(new TickListener() {
      @Override
      public void tick(TimeLapse timeLapse) {
        if (timeLapse.getStartTime() == timeLapse.getTickLength() * t) {
          try {
            Thread.sleep(150);
          } catch (final InterruptedException e) {
            throw new IllegalStateException(e);
          }
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });
    boolean fail = false;
    try {
      getModel().start();
    } catch (final IllegalStateException e) {
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  /**
   * Tests repeatedly switching between fast forward and real time mode.
   */
  @Test
  public void testSwitching() {
    assertThat(getModel().getClockMode()).isEqualTo(ClockMode.REAL_TIME);
    getModel().switchToSimulatedTime();
    assertThat(getModel().getClockMode()).isEqualTo(ClockMode.SIMULATED);
    final ListenerEventHistory history = new ListenerEventHistory();
    getModel().getEventAPI().addListener(history,
        ImmutableSet.<Enum<?>>builder()
            .add(ClockEventType.values())
            .add(RtClockEventType.values())
            .build());

    final List<Long> times = new ArrayList<>();
    final List<Long> timeLapseTimes = new ArrayList<>();
    getModel().register(new TickListener() {
      @Override
      public void tick(TimeLapse timeLapse) {
        timeLapseTimes.add(timeLapse.getStartTime());
        // start in sim time
        if (timeLapse.getTime() == 0) {
          assertThat(history.getEventTypeHistory()).containsExactly(
              ClockEventType.STARTED, RtClockEventType.SWITCH_TO_SIM_TIME);
        }

        // initiate switch from simulated to real time
        if (timeLapse.getTime() == 100000 || timeLapse.getTime() == 200000) {
          if (timeLapse.getTime() == 100000) {
            assertThat(history.getEventTypeHistory()).containsExactly(
                ClockEventType.STARTED, RtClockEventType.SWITCH_TO_SIM_TIME);
          } else {
            assertThat(history.getEventTypeHistory()).containsExactly(
                ClockEventType.STARTED,
                RtClockEventType.SWITCH_TO_SIM_TIME,
                RtClockEventType.SWITCH_TO_REAL_TIME,
                RtClockEventType.SWITCH_TO_SIM_TIME);
          }
          times.add(System.nanoTime());
          assertThat(getModel().getClockMode()).isEqualTo(ClockMode.SIMULATED);
          getModel().switchToRealTime();
          getModel().switchToSimulatedTime();
          getModel().switchToRealTime();
          assertThat(getModel().getClockMode()).isEqualTo(ClockMode.SIMULATED);
          if (timeLapse.getTime() == 100000) {
            assertThat(history.getEventTypeHistory()).containsExactly(
                ClockEventType.STARTED, RtClockEventType.SWITCH_TO_SIM_TIME);
          } else {
            assertThat(history.getEventTypeHistory()).containsExactly(
                ClockEventType.STARTED,
                RtClockEventType.SWITCH_TO_SIM_TIME,
                RtClockEventType.SWITCH_TO_REAL_TIME,
                RtClockEventType.SWITCH_TO_SIM_TIME);
          }

        }
        // switch to real time should be completed
        if (timeLapse.getTime() == 100100 || timeLapse.getTime() == 200100) {
          assertThat(getModel().getClockMode()).isEqualTo(ClockMode.REAL_TIME);
          if (timeLapse.getTime() == 100100) {
            assertThat(history.getEventTypeHistory()).containsExactly(
                ClockEventType.STARTED,
                RtClockEventType.SWITCH_TO_SIM_TIME,
                RtClockEventType.SWITCH_TO_REAL_TIME);
          } else {
            assertThat(history.getEventTypeHistory()).containsExactly(
                ClockEventType.STARTED,
                RtClockEventType.SWITCH_TO_SIM_TIME,
                RtClockEventType.SWITCH_TO_REAL_TIME,
                RtClockEventType.SWITCH_TO_SIM_TIME,
                RtClockEventType.SWITCH_TO_REAL_TIME);
          }
        }

        // this switch should not have any effect
        if (timeLapse.getTime() == 50000) {
          assertThat(getModel().getClockMode()).isEqualTo(ClockMode.SIMULATED);
          getModel().switchToRealTime();
          assertThat(getModel().getClockMode()).isEqualTo(ClockMode.SIMULATED);
          getModel().switchToSimulatedTime();
          assertThat(getModel().getClockMode()).isEqualTo(ClockMode.SIMULATED);
        }
        // initiate switch from real time to simulated time
        if (timeLapse.getTime() == 100500 || timeLapse.getTime() == 200500) {
          times.add(System.nanoTime());
          assertThat(getModel().getClockMode()).isEqualTo(ClockMode.REAL_TIME);
          getModel().switchToSimulatedTime();
          getModel().switchToRealTime();
          getModel().switchToSimulatedTime();
          assertThat(getModel().getClockMode()).isEqualTo(ClockMode.REAL_TIME);
          if (timeLapse.getTime() == 100500) {
            assertThat(history.getEventTypeHistory()).containsExactly(
                ClockEventType.STARTED,
                RtClockEventType.SWITCH_TO_SIM_TIME,
                RtClockEventType.SWITCH_TO_REAL_TIME);
          } else {
            assertThat(history.getEventTypeHistory()).containsExactly(
                ClockEventType.STARTED,
                RtClockEventType.SWITCH_TO_SIM_TIME,
                RtClockEventType.SWITCH_TO_REAL_TIME,
                RtClockEventType.SWITCH_TO_SIM_TIME,
                RtClockEventType.SWITCH_TO_REAL_TIME);
          }
        }
        // now the switch to simulated should be completed
        if (timeLapse.getTime() == 100600 || timeLapse.getTime() == 200600) {
          assertThat(getModel().getClockMode()).isEqualTo(ClockMode.SIMULATED);
          if (timeLapse.getTime() == 100600) {
            assertThat(history.getEventTypeHistory()).containsExactly(
                ClockEventType.STARTED,
                RtClockEventType.SWITCH_TO_SIM_TIME,
                RtClockEventType.SWITCH_TO_REAL_TIME,
                RtClockEventType.SWITCH_TO_SIM_TIME);
          } else {
            assertThat(history.getEventTypeHistory()).containsExactly(
                ClockEventType.STARTED,
                RtClockEventType.SWITCH_TO_SIM_TIME,
                RtClockEventType.SWITCH_TO_REAL_TIME,
                RtClockEventType.SWITCH_TO_SIM_TIME,
                RtClockEventType.SWITCH_TO_REAL_TIME,
                RtClockEventType.SWITCH_TO_SIM_TIME);
          }
        }
        // this switch should not have any effect
        if (timeLapse.getTime() == 100200) {
          assertThat(getModel().getClockMode()).isEqualTo(ClockMode.REAL_TIME);
          getModel().switchToSimulatedTime();
          assertThat(getModel().getClockMode()).isEqualTo(ClockMode.REAL_TIME);
          getModel().switchToRealTime();
          assertThat(getModel().getClockMode()).isEqualTo(ClockMode.REAL_TIME);
        }
        if (timeLapse.getTime() >= 300000) {
          times.add(System.nanoTime());
          assertThat(getModel().isTicking()).isTrue();
          assertThat(getModel().getClockMode()).isEqualTo(ClockMode.SIMULATED);
          getModel().stop();
          assertThat(getModel().isTicking()).isFalse();
          assertThat(getModel().getClockMode()).isEqualTo(ClockMode.STOPPED);
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });

    assertThat(times).isEmpty();
    assertThat(timeLapseTimes).isEmpty();

    getModel().start();
    assertThat(getModel().getClockMode()).isEqualTo(ClockMode.STOPPED);
    assertThat(history.getEventTypeHistory()).containsExactly(
        ClockEventType.STARTED,
        RtClockEventType.SWITCH_TO_SIM_TIME,
        RtClockEventType.SWITCH_TO_REAL_TIME,
        RtClockEventType.SWITCH_TO_SIM_TIME,
        RtClockEventType.SWITCH_TO_REAL_TIME,
        RtClockEventType.SWITCH_TO_SIM_TIME,
        ClockEventType.STOPPED);

    assertThat(times).hasSize(5);
    assertThat(timeLapseTimes).hasSize(3001);

    final PeekingIterator<Long> it = Iterators
        .peekingIterator(times.iterator());

    final List<Double> interArrivalTimes = new ArrayList<>();
    for (long l1 = it.next(); it.hasNext(); l1 = it.next()) {
      final Long l2 = it.peek();
      interArrivalTimes.add((l2 - l1) / 1000000d);
    }
    assertThat(interArrivalTimes.get(0)).isAtLeast(400d);
    assertThat(interArrivalTimes.get(0)).isAtMost(500d);

    assertThat(interArrivalTimes.get(1)).isAtMost(500d);

    assertThat(interArrivalTimes.get(2)).isAtLeast(400d);
    assertThat(interArrivalTimes.get(2)).isAtMost(500d);

    assertThat(interArrivalTimes.get(3)).isAtMost(500d);
  }

  /**
   * Test that a tick listener that takes too much time is detected.
   */
  @Test
  public void testTimingChecker() {
    getModel().register(new TickListener() {
      @Override
      public void tick(TimeLapse timeLapse) {
        try {
          Thread.sleep(111L);
        } catch (final InterruptedException e) {
          throw new IllegalStateException(e);
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });
    boolean fail = false;
    try {
      getModel().start();
    } catch (final IllegalStateException e) {
      assertThat(e.getMessage()).contains("took too much time");
      fail = true;
    }
    assertThat(fail).isTrue();
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
  }

}
