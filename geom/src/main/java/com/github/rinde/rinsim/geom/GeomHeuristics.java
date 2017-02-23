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
package com.github.rinde.rinsim.geom;

import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.google.common.base.Optional;

/**
 * Default {@link GeomHeuristic} implementations.
 * @author Rinde van Lon
 */
public final class GeomHeuristics {
  private GeomHeuristics() {}

  /**
   * @return Euclidean distance implementation.
   */
  public static GeomHeuristic euclidean() {
    return StaticHeuristics.EUCLIDEAN;
  }

  /**
   * Creates a heuristic that calculates cost using travel time.
   * @param defaultMaxSpeed The default maximum speed, this value is used in
   *          case a connection doesn't specify a speed.
   * @return A new instance.
   */
  public static GeomHeuristic time(double defaultMaxSpeed) {
    return new TimeGraphHeuristic(defaultMaxSpeed);
  }

  /**
   * Creates a heuristic that calculates cost using theoretical travel time.
   * @param defaultMaxSpeed The default maximum speed, this value is used in
   *          case a connection doesn't specify a speed.
   * @return A new instance.
   */
  public static GeomHeuristic theoreticalTime(double defaultMaxSpeed) {
    return new TheoreticalTimeGraphHeuristic(defaultMaxSpeed);
  }

  enum StaticHeuristics implements GeomHeuristic {

    EUCLIDEAN {
      @Override
      public double calculateCost(Graph<?> graph, Point from, Point to) {
        return graph.connectionLength(from, to);
      }

      @Override
      public double estimateCost(Graph<?> graph, Point from, Point to) {
        return Point.distance(from, to);
      }

      @Override
      public String toString() {
        return GeomHeuristics.class.getSimpleName() + ".euclidean()";
      }

      @Override
      public double calculateTravelTime(Graph<?> graph, Point from, Point to,
          Unit<Length> distanceUnit,
          Measure<Double, Velocity> speed, Unit<Duration> outputTimeUnit) {
        final Measure<Double, Length> distance = Measure
          .valueOf(graph.getConnection(from, to).getLength(), distanceUnit);

        return Measure.valueOf(distance.doubleValue(SI.METER)
          // divided by m/s
          / speed.doubleValue(SI.METERS_PER_SECOND),
          // gives seconds
          SI.SECOND)
          // convert to desired unit
          .doubleValue(outputTimeUnit);
      }
    }
  }

  abstract static class AbstractMadGraphHeuristic implements GeomHeuristic {
    static final String R_BRACE = ")";

    @Nullable
    private Graph<?> lastGraph;
    private boolean safeToCast;

    @Nullable
    MultiAttributeData getData(Graph<?> graph, Point from, Point to) {
      if (graph.hasConnection(from, to)) {
        final Connection<?> conn = graph.getConnection(from, to);
        safeToCast = lastGraph == graph && safeToCast;
        lastGraph = graph;

        if (conn.data().isPresent()
          && (safeToCast || conn.data().get() instanceof MultiAttributeData)) {
          safeToCast = true;
          return (MultiAttributeData) conn.data().get();
        }
      }
      return null;
    }

    @Override
    public double calculateTravelTime(Graph<?> graph, Point from, Point to,
        Unit<Length> distanceUnit,
        Measure<Double, Velocity> speed, Unit<Duration> outputTimeUnit) {
      final Measure<Double, Length> distance =
        Measure.valueOf(graph.connectionLength(from, to), distanceUnit);
      return Math.min(doCalculateTravelTime(speed, distance, outputTimeUnit),
        doCalculateTravelTime(
          Measure.valueOf(getSpeed(graph, from, to), speed.getUnit()), distance,
          outputTimeUnit));
    }

    double doCalculateTravelTime(Measure<Double, Velocity> speed,
        Measure<Double, Length> distance, Unit<Duration> outputTimeUnit) {
      return Measure.valueOf(distance.doubleValue(SI.METER)
        // divided by m/s
        / speed.doubleValue(SI.METERS_PER_SECOND),
        // gives seconds
        SI.SECOND)
        // convert to desired unit
        .doubleValue(outputTimeUnit);
    }

    @Override
    public double estimateCost(Graph<?> graph, Point from, Point to) {
      final double speed = getSpeed(graph, from, to);
      final double length = Point.distance(from, to);
      // // Potential metric conflict, shouldn't affect relative outcome
      return length / speed;
    }

    @Override
    public double calculateCost(Graph<?> graph, Point from, Point to) {
      final double speed = getSpeed(graph, from, to);
      final double length = graph.connectionLength(from, to);
      // Potential metric conflict, shouldn't affect relative outcome
      return length / speed;
    }

    abstract double getSpeed(Graph<?> graph, Point from, Point to);

  }

  static class TimeGraphHeuristic extends AbstractMadGraphHeuristic {
    private final double defaultMaxSpeed;

    TimeGraphHeuristic(double defaultMxSpeed) {
      defaultMaxSpeed = defaultMxSpeed;
    }

    @Override
    double getSpeed(Graph<?> graph, Point from, Point to) {
      final MultiAttributeData data = getData(graph, from, to);
      if (data != null) {
        final Optional<Double> mxSpeed = data.getMaxSpeed();
        if (mxSpeed.isPresent()) {
          return mxSpeed.get();
        }
      }
      return defaultMaxSpeed;
    }

    @Override
    public String toString() {
      return GeomHeuristics.class.getSimpleName() + ".time(" + defaultMaxSpeed
        + R_BRACE;
    }
  }

  static class TheoreticalTimeGraphHeuristic extends AbstractMadGraphHeuristic {
    private final double defaultMaxSpeed;

    TheoreticalTimeGraphHeuristic(double speed) {
      defaultMaxSpeed = speed;
    }

    @Override
    double getSpeed(Graph<?> graph, Point from, Point to) {
      final MultiAttributeData data = getData(graph, from, to);
      if (data != null && data.getAttributes()
        .containsKey(MultiAttributeData.THEORETICAL_SPEED_ATTRIBUTE)) {
        return Double.parseDouble(data.getAttributes()
          .get(MultiAttributeData.THEORETICAL_SPEED_ATTRIBUTE)
          .toString());
      }
      return defaultMaxSpeed;
    }

    @Override
    public String toString() {
      return GeomHeuristics.class.getSimpleName() + ".theoreticalTime("
        + defaultMaxSpeed + R_BRACE;
    }
  }
}
