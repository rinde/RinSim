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

import javax.measure.quantity.Duration;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.core.TickListener;

/**
 * @author Rinde van Lon
 *
 */
public class TickListenerChecker implements TickListener {

  private final long expectedTimeStep;
  private final Unit<Duration> expectedTimeUnit;
  private long tickCount;
  private long afterTickCount;
  private long lastTickTime;
  private long lastAfterTickTime;

  public TickListenerChecker(TimeModel tm) {
    this(tm.getTimeStep(), tm.getTimeUnit());
  }

  public TickListenerChecker() {
    this(1000L, SI.MILLI(SI.SECOND));
  }

  public TickListenerChecker(long expectedStep, Unit<Duration> expectedUnit) {
    expectedTimeStep = expectedStep;
    expectedTimeUnit = expectedUnit;
    tickCount = 0L;
    afterTickCount = 0L;
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    assertThat(timeLapse.getTimeUnit()).isEqualTo(expectedTimeUnit);
    assertThat(timeLapse.getTimeStep()).isEqualTo(expectedTimeStep);
    tickCount++;
    lastTickTime = System.nanoTime();
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {
    assertThat(timeLapse.getTimeUnit()).isEqualTo(expectedTimeUnit);
    assertThat(timeLapse.getTimeStep()).isEqualTo(expectedTimeStep);
    afterTickCount++;
    lastAfterTickTime = System.nanoTime();
  }

  public void assertCountEquals() {
    assertThat(tickCount).comparesEqualTo(afterTickCount);
  }

  public void assertCountEquals(long c) {
    assertCountEquals();
    assertThat(tickCount).comparesEqualTo(c);
  }

  public void assertTickOrder() {
    assertThat(lastAfterTickTime - lastTickTime).isAtLeast(0L);
  }

  /**
   * @return the expected
   */
  public Unit<Duration> getExpected() {
    return expectedTimeUnit;
  }

  /**
   * @return the tickCount
   */
  public long getTickCount() {
    return tickCount;
  }

  /**
   * @return the afterTickCount
   */
  public long getAfterTickCount() {
    return afterTickCount;
  }

  /**
   * @return the lastTickTime
   */
  public long getLastTickTime() {
    return lastTickTime;
  }

  /**
   * @return the lastAfterTickTime
   */
  public long getLastAfterTickTime() {
    return lastAfterTickTime;
  }
}
