package com.github.rinde.rinsim.scenario.generator;

import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.generator.ScenarioGenerator.TravelTimes;

public class TravelTimesUtil {

  public static TravelTimes constant(long cons) {
    return new ConstantTT(cons);
  }

  public static TravelTimes distance() {
    return DistanceTT.INSTANCE;
  }

  static class ConstantTT implements TravelTimes {
    private final long constValue;

    public ConstantTT(long value) {
      constValue = value;
    }

    @Override
    public long getShortestTravelTime(Point from, Point to) {
      return constValue;
    }

    @Override
    public long getTravelTimeToNearestDepot(Point from) {
      return constValue;
    }
  }

  static enum DistanceTT implements TravelTimes {
    INSTANCE {
      private final Point DEPOT_LOC = new Point(0, 0);

      @Override
      public long getShortestTravelTime(Point from, Point to) {
        return (long) Point.distance(from, to);
      }

      @Override
      public long getTravelTimeToNearestDepot(Point from) {
        return (long) Point.distance(from, DEPOT_LOC);
      }
    }
  }
}
