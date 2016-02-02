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
package com.github.rinde.rinsim.core.model.time;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.github.rinde.rinsim.core.model.time.Clock.ClockEventType;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.RtClockEventType;
import com.github.rinde.rinsim.core.model.time.TimeModel.AbstractBuilder;
import com.github.rinde.rinsim.event.ListenerEventHistory;
import com.github.rinde.rinsim.testutil.RealtimeTests;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Range;

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
    final long duration = System.nanoTime() - start;// - Realtime.STARTUP_DELAY;
    // duration should be somewhere between 200 and 300 ms
    assertThat(duration).isAtLeast(200000000L);
    assertThat(duration).isLessThan(300000000L);
    assertThat(getModel().getCurrentTime()).isEqualTo(300);
    assertThat(getModel().isExecutorAlive()).isFalse();
  }

  /**
   * Tests tick durations.
   */
  @Test
  public void timingDurationTest() {
    getModel().switchToSimulatedTime();

    final List<Long> timeStamps = new ArrayList<>();
    final List<Long> simTimeStamps = new ArrayList<>();
    final List<ClockMode> modes = new ArrayList<>();
    getModel().register(new TickListener() {
      @Override
      public void tick(TimeLapse timeLapse) {
        timeStamps.add(System.nanoTime());
        simTimeStamps.add(timeLapse.getStartTime());

        if (timeLapse.getStartTime() == 200) {
          getModel().switchToRealTime();
        } else if (timeLapse.getStartTime() == 500) {
          getModel().switchToSimulatedTime();
        }
        modes.add(getModel().getClockMode());
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });
    getModel().register(new LimitingTickListener(getModel(), 8));

    getModel().start();

    final PeekingIterator<Long> it =
      Iterators.peekingIterator(timeStamps.iterator());
    final List<Double> interArrivalTimes = new ArrayList<>();
    for (long l1 = it.next(); it.hasNext(); l1 = it.next()) {
      final long l2 = it.peek();
      interArrivalTimes.add((l2 - l1) / 1000000d);
    }

    // time | interarrival index
    // 0
    // ------- 0. simulated time
    // 100
    // ------- 1. simulated time
    // 200
    // ------- 2. simulated time -> initiate switch to real-time
    // 300
    // ------- 3. duration must be ~100ms
    // 400
    // ------- 4. duration must be ~100ms
    // 500
    // ------- 5. switch back to simulated time
    // 600
    // ------- 6. simulated time
    // 700

    assertThat(sum(interArrivalTimes.subList(0, 3))).isAtMost(100d);
    assertThat(interArrivalTimes.get(3)).isIn(Range.openClosed(90d, 110d));
    assertThat(interArrivalTimes.get(4)).isIn(Range.openClosed(90d, 110d));
    assertThat(sum(interArrivalTimes.subList(5, 7))).isAtMost(100d);
    assertThat(getModel().isExecutorAlive()).isFalse();
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
          // this switch should be ignored
          getModel().switchToSimulatedTime();
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
          getModel().switchToSimulatedTime();
          assertThat(getModel().getClockMode()).isEqualTo(ClockMode.SIMULATED);
        }
        // initiate switch from real time to simulated time
        if (timeLapse.getTime() == 100500 || timeLapse.getTime() == 200500) {
          times.add(System.nanoTime());
          assertThat(getModel().getClockMode()).isEqualTo(ClockMode.REAL_TIME);
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
    assertThat(getModel().isExecutorAlive()).isFalse();
  }

  static double sum(List<Double> list) {
    double sum = 0d;
    for (final double l : list) {
      sum += l;
    }
    return sum;
  }

}
