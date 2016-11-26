package com.github.rinde.rinsim.central;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.geom.Point;

abstract class AbstractTravelTimes implements TravelTimes {

  final Unit<Duration> timeUnit;
  final Unit<Length> distanceUnit;

  AbstractTravelTimes(Unit<Duration> tu,
      Unit<Length> du) {
    timeUnit = tu;
    distanceUnit = du;
  }

  AbstractTravelTimes(AbstractTravelTimes tt) {
    timeUnit = tt.timeUnit;
    distanceUnit = tt.distanceUnit;
  }

  @Override
  public abstract long getTheoreticalShortestTravelTime(Point from, Point to);

  @Override
  public abstract long getCurrentShortestTravelTime(Point from, Point to);

  @Override
  public abstract double computeTheoreticalDistance(Point from, Point to);

  @Override
  public abstract double computeCurrentDistance(Point from, Point to);

}
