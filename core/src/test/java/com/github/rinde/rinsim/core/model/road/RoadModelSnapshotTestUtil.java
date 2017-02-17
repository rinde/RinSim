/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.core.model.road;

import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.ImmutableGraph;
import com.github.rinde.rinsim.geom.Point;

/**
 * Utility methods for the creation of snapshots of potentially non-existing
 * road models.
 * @author Vincent Van Gestel
 */
public class RoadModelSnapshotTestUtil {

  /**
   * Creates a snapshot of a {@link PlaneRoadModel}.
   * @param minimum The minimum bound of the plane.
   * @param maximum The maximum bound of the plane.
   * @param planeDistanceUnit The distance unit of the model.
   * @return A snapshot with the desired properties.
   */
  public static RoadModelSnapshot createPlaneRoadModelSnapshot(
      Point minimum,
      Point maximum, Unit<Length> planeDistanceUnit) {
    return new AutoValue_PlaneRoadModelSnapshot(
      new PlaneRoadModel(RoadModelBuilders.plane().withMinPoint(minimum)
        .withMaxPoint(maximum).withDistanceUnit(planeDistanceUnit)));
  }

  /**
   * Creates a snapshot of a {@link GraphRoadModel}.
   * @param graph The graph to snapshot.
   * @param modelDistanceUnit The distance unit of the model.
   * @return A snapshot with the desired properties.
   */
  public static GraphRoadModelSnapshot createGraphRoadModelSnapshot(
      Graph<?> graph,
      Unit<Length> modelDistanceUnit) {
    return new AutoValue_GraphRoadModelSnapshot(ImmutableGraph.copyOf(graph),
      modelDistanceUnit);
  }
}
