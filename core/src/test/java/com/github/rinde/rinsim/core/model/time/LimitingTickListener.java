package com.github.rinde.rinsim.core.model.time;

import static com.google.common.truth.Truth.assertThat;

import com.github.rinde.rinsim.core.TickListener;

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
