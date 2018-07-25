

package com.github.rinde.rinsim.scenario;

import com.github.rinde.rinsim.geom.Point;
import javax.annotation.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ScenarioTest_AddObjectEvent extends ScenarioTest.AddObjectEvent {

  private final long time;

  private final Point point;

  AutoValue_ScenarioTest_AddObjectEvent(
      long time,
      Point point) {
    this.time = time;
    if (point == null) {
      throw new NullPointerException("Null point");
    }
    this.point = point;
  }

  @Override
  public long getTime() {
    return time;
  }

  @Override
  Point getPoint() {
    return point;
  }

  @Override
  public String toString() {
    return "AddObjectEvent{"
         + "time=" + time + ", "
         + "point=" + point
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ScenarioTest.AddObjectEvent) {
      ScenarioTest.AddObjectEvent that = (ScenarioTest.AddObjectEvent) o;
      return (this.time == that.getTime())
           && (this.point.equals(that.getPoint()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= (int) ((time >>> 32) ^ time);
    h$ *= 1000003;
    h$ ^= point.hashCode();
    return h$;
  }

}
