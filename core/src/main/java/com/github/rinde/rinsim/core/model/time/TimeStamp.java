package com.github.rinde.rinsim.core.model.time;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class TimeStamp {

  TimeStamp() {}

  public abstract long getTickCount();

  public abstract long getMillis();

  public abstract long getNanos();

  static TimeStamp now(long tickCount) {
    return create(tickCount, System.currentTimeMillis(), System.nanoTime());
  }

  static TimeStamp create(long tickCount, long millis, long nanos) {
    return new AutoValue_TimeStamp(tickCount, millis, nanos);
  }
}
