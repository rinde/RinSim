/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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
