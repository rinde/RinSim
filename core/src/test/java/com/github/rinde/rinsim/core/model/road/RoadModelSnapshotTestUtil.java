package com.github.rinde.rinsim.core.model.road;

import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;

public class RoadModelSnapshotTestUtil {

  public static RoadModelSnapshot createPlaneRoadModelSnapshot(
      Point minimum,
      Point maximum, Unit<Length> planeDistanceUnit) {
    return new PlaneModelSnapshot(minimum, maximum, planeDistanceUnit);
  }

  public static GraphModelSnapshot createGraphRoadModelSnapshot(Graph<?> graph,
      Unit<Length> modelDistanceUnit) {
    return new GraphModelSnapshot(graph, modelDistanceUnit);
  }
}
