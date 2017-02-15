package com.github.rinde.rinsim.core.model.road;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Iterator;
import java.util.List;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.HeuristicPath;
import com.github.rinde.rinsim.geom.ImmutableGraph;
import com.github.rinde.rinsim.geom.Point;

final class GraphModelSnapshot extends AbstractModelSnapshot {

  private final ImmutableGraph<? extends ConnectionData> graph;

  public GraphModelSnapshot(Graph<? extends ConnectionData> snapshotGraph,
      Unit<Length> modelDistanceUnit) {
    super(modelDistanceUnit);
    graph = ImmutableGraph.copyOf(snapshotGraph);
  }

  @Override
  public HeuristicPath getPathTo(Point from, Point to, Unit<Duration> timeUnit,
      Measure<Double, Velocity> speed, Graphs.Heuristic heuristic) {
    final List<Point> path =
      Graphs.shortestPath(graph, from, to, heuristic);

    final Iterator<Point> pathIt = path.iterator();

    double cost = 0d;
    double travelTime = 0d;
    Point prev = pathIt.next();
    while (pathIt.hasNext()) {
      final Point cur = pathIt.next();
      cost += heuristic.calculateCost(graph, prev, cur);
      travelTime += heuristic.calculateTravelTime(graph, prev, cur,
        distanceUnit, speed, timeUnit);
      prev = cur;
    }
    return new HeuristicPath(path, cost, travelTime);
  }

  @Override
  public Measure<Double, Length> getDistanceOfPath(Iterable<Point> path) {
    final Iterator<Point> pathIt = path.iterator();
    checkArgument(pathIt.hasNext(), "Cannot check distance of an empty path.");
    Point prev = pathIt.next();
    Point cur = null;
    double distance = 0d;
    while (pathIt.hasNext()) {
      cur = pathIt.next();
      distance += graph.connectionLength(prev, cur);
      prev = cur;
    }
    return Measure.valueOf(distance, distanceUnit);
  }

}
