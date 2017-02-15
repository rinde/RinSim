package com.github.rinde.rinsim.core.model.road;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.HeuristicPath;
import com.github.rinde.rinsim.geom.Point;

public interface RoadModelSnapshot {

  HeuristicPath getPathTo(Point from, Point to, Unit<Duration> timeUnit,
      Measure<Double, Velocity> speed, Graphs.Heuristic heuristic);

  Measure<Double, Length> getDistanceOfPath(Iterable<Point> path);

}
