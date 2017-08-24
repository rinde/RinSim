package com.github.rinde.rinsim.pdptw.common;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.event.Event;

/**
 * Event dispatched by a {@link StatsProvider} indicating that some
 * statistic has changed.
 * @author Rinde van Lon
 */
public class StatsEvent extends Event {
  private final Parcel parcel;
  private final Vehicle vehicle;
  private final long tardiness;
  private final long time;

  StatsEvent(Enum<?> type, Object pIssuer, Parcel p, Vehicle v,
      long tar, long tim) {
    super(type, pIssuer);
    parcel = p;
    vehicle = v;
    tardiness = tar;
    time = tim;
  }

  /**
   * @return The parcel which pickup or delivery is tardy.
   */
  public Parcel getParcel() {
    return parcel;
  }

  /**
   * @return The vehicle that caused the tardiness.
   */
  public Vehicle getVehicle() {
    return vehicle;
  }

  /**
   * @return The tardiness.
   */
  public long getTardiness() {
    return tardiness;
  }

  /**
   * @return Current time.
   */
  public long getTime() {
    return time;
  }
}
