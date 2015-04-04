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

class LimitingTickListener implements TickListener {
  private final int limit;
  private int tickCount;
  private final Clock clock;

  public LimitingTickListener(Clock s, int tickLimit) {
    clock = s;
    limit = tickLimit;
    tickCount = 0;
  }

  public void reset() {
    tickCount = 0;
  }

  @Override
  public void tick(TimeLapse tl) {
    tickCount++;
  }

  @Override
  public void afterTick(TimeLapse tl) {
    if (tickCount >= limit) {
      assertThat(clock.isTicking()).isTrue();
      clock.stop();
      assertThat(clock.isTicking()).isFalse();
      reset();
    }
  }
}
