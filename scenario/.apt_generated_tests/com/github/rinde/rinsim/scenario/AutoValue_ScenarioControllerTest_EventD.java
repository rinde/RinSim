

package com.github.rinde.rinsim.scenario;

import javax.annotation.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ScenarioControllerTest_EventD extends ScenarioControllerTest.EventD {

  private final long time;

  AutoValue_ScenarioControllerTest_EventD(
      long time) {
    this.time = time;
  }

  @Override
  public long getTime() {
    return time;
  }

  @Override
  public String toString() {
    return "EventD{"
         + "time=" + time
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ScenarioControllerTest.EventD) {
      ScenarioControllerTest.EventD that = (ScenarioControllerTest.EventD) o;
      return (this.time == that.getTime());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= (int) ((time >>> 32) ^ time);
    return h$;
  }

}
