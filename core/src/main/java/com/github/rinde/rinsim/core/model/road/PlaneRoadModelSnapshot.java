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
package com.github.rinde.rinsim.core.model.road;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.geom.GeomHeuristic;
import com.github.rinde.rinsim.geom.Point;
import com.google.auto.value.AutoValue;

/**
 * The snapshot of a {@link PlaneRoadModel}.
 * @author Vincent Van Gestel
 */
@AutoValue
abstract class PlaneRoadModelSnapshot
    implements RoadModelSnapshot {

  PlaneRoadModelSnapshot() {}

  public abstract PlaneRoadModel getModel();

  @Override
  public RoadPath getPathTo(Point from, Point to, Unit<Duration> timeUnit,
      Measure<Double, Velocity> speed, GeomHeuristic heuristic) {
    return getModel().getPathTo(from, to, timeUnit, speed, heuristic);
  }

  @Override
  public Measure<Double, Length> getDistanceOfPath(Iterable<Point> path)
      throws IllegalArgumentException {
    return getModel().getDistanceOfPath(path);
  }

  static PlaneRoadModelSnapshot create(PlaneRoadModel model) {
    return new AutoValue_PlaneRoadModelSnapshot(model);
  }
}
