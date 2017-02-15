package com.github.rinde.rinsim.geom;

import java.util.List;

public final class HeuristicPath {

  public final List<Point> path;
  public final double value;
  public final double travelTime;

  public HeuristicPath(List<Point> pointPath, double heuristicValue,
      double pathTravelTime) {
    path = pointPath;
    value = heuristicValue;
    travelTime = pathTravelTime;
  }

}
