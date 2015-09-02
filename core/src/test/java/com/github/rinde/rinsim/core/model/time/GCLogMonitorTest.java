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

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.model.time.GCLogMonitor.PauseTime;

/**
 *
 * @author Rinde van Lon
 */
public class GCLogMonitorTest {

  GCLogMonitor logMonitor;

  @Before
  public void setUp() {
    logMonitor = GCLogMonitor.getInstance();
  }

  @Test
  public void test() {
    final long startTime = System.currentTimeMillis();
    assertThat(logMonitor.hasSurpassed(startTime + 500)).isFalse();

    List<Object> objects = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      objects.add(new Object());
    }
    objects = null;
    System.gc();
    sleep(300);
    System.gc();
    sleep(3000);

    logMonitor.checkInternalState();

    // after two GC calls and sleeps we are confident that we will find some
    // evidence of GC activity in the log
    assertThat(logMonitor.hasSurpassed(startTime)).isTrue();
    assertThat(logMonitor.getPauseTimeInInterval(startTime,
        System.currentTimeMillis())).isGreaterThan(0L);
  }

  @Test
  public void constructTest() {
    // needed for AutoValue
    final long time = 123;
    final long duration = 456;
    final PauseTime pt = PauseTime.create(time, duration);
    assertThat(pt.getTimeMs()).isEqualTo(time);
    assertThat(pt.getDurationNs()).isEqualTo(duration);
  }

  static void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (final InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

}
