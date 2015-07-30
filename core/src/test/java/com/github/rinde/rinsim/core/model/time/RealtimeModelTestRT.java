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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.github.rinde.rinsim.core.model.time.TimeModel.AbstractBuilder;
import com.github.rinde.rinsim.testutil.RealtimeTests;
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
    final long duration = System.nanoTime() - start;
    // duration should be somewhere between 200 and 300 ms
    assertThat(duration).isAtLeast(200000000L);
    assertThat(duration).isLessThan(300000000L);
    assertThat(getModel().getCurrentTime()).isEqualTo(300);
  }

  /**
   * Tests tick durations.
   */
  @Test
  public void timingDurationTest() {
    getModel().switchToSimulatedTime();

    final List<Long> timeStamps = new ArrayList<>();
    final List<Long> simTimeStamps = new ArrayList<>();
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
    for (int i = 0; i < simTimeStamps.size(); i++) {

      if (i == 0) {
        System.out.println(simTimeStamps.get(i));
      } else {
        System.out.println("\t" + interArrivalTimes.get(i - 1));
        System.out.println(simTimeStamps.get(i));
      }
    }

  }

  static double sum(List<Double> list) {
    double sum = 0d;
    for (final double l : list) {
      sum += l;
    }
    return sum;
  }

}
