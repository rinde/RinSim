package com.github.rinde.rinsim.core.model.time;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class MeasuredDeviation {

  MeasuredDeviation() {}

  public abstract long getDeviationNs();

  public abstract long getCorrectionNs();

  public abstract TimeStamp getTimeStamp();

  static MeasuredDeviation create(long dev, long corr, TimeStamp ts) {
    return new AutoValue_MeasuredDeviation(dev, corr, ts);
  }
}
