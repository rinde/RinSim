/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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

import static com.google.common.base.Verify.verifyNotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;

/**
 * Point represents a position in euclidean space, it is defined as a simple
 * tuple (double,double).
 * @author Rinde van Lon
 */
public class Point implements Serializable {
  private static final String NUM_SEPARATOR = ",";

  private static final long serialVersionUID = -7501053764573661924L;
  /**
   * The x coordinate.
   */
  public final double x;

  /**
   * The y coordinate.
   */
  public final double y;

  private final int hashCode;

  /**
   * Create a new point.
   * @param pX The x coordinate.
   * @param pY The y coordinate.
   */
  public Point(double pX, double pY) {
    x = pX;
    y = pY;
    hashCode = Objects.hashCode(x, y);
  }

  /**
   * Computes the distance between two points.
   * @param p1 A point.
   * @param p2 Another point.
   * @return The distance between the two points.
   */
  public static double distance(Point p1, Point p2) {
    final double dx = p1.x - p2.x;
    final double dy = p1.y - p2.y;
    return Math.sqrt(dx * dx + dy * dy);
  }

  /**
   * Computes the sum between two points: <code>p1 + p2</code>.
   * @param p1 A point.
   * @param p2 Another point.
   * @return The sum as a point object.
   */
  public static Point add(Point p1, Point p2) {
    return new Point(p1.x + p2.x, p1.y + p2.y);
  }

  /**
   * Computes the difference between two points: <code>p1 - p2</code>.
   * @param p1 A point.
   * @param p2 Another point.
   * @return The difference as a point object.
   */
  public static Point diff(Point p1, Point p2) {
    return new Point(p1.x - p2.x, p1.y - p2.y);
  }

  /**
   * Multiplies the point, the result is returned as a new point.
   * @param p The point to multiply.
   * @param d The multiplicand.
   * @return A new point containing the result.
   */
  public static Point multiply(Point p, double d) {
    return new Point(p.x * d, p.y * d);
  }

  /**
   * Divides the point, the result is returned as a new point.
   * @param p The point to divide.
   * @param d The divisor.
   * @return A new point containing the result.
   */
  public static Point divide(Point p, double d) {
    return new Point(p.x / d, p.y / d);
  }

  /**
   * Returns the center of a list of points.
   * @param points The list of points
   * @return A new point indicating the center of the given points.
   */
  public static Point center(Collection<Point> points) {
    Point sumPoint = new Point(0, 0);
    for (final Point p : points) {
      sumPoint = Point.add(sumPoint, p);
    }
    return divide(sumPoint, points.size());
  }

  /**
   * Parses the specified string to a point. Example inputs:
   * <ul>
   * <li><code>(10.0,2)</code></li>
   * <li><code>5,6</code></li>
   * <li><code>(7.3242349832,0</code></li>
   * </ul>
   * @param pointString The string to parse.
   * @return A point.
   */
  public static Point parsePoint(String pointString) {
    final String[] parts = pointString.replaceAll("\\(|\\)", "").split(
      NUM_SEPARATOR);
    return new Point(Double.parseDouble(parts[0]),
      Double.parseDouble(parts[1]));
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other == null) {
      return false;
    }
    if (other == this) {
      return true;
    }
    // allows comparison with subclasses
    if (!(other instanceof Point)) {
      return false;
    }
    final Point p = (Point) other;
    return x == p.x && y == p.y;
  }

  @Override
  public String toString() {
    return new StringBuilder().append("(").append(x).append(NUM_SEPARATOR)
      .append(y).append(")").toString();
  }

  /**
   * Provides default comparators for comparing {@link Point}s.
   * @author Rinde van Lon
   */
  public enum Comparators implements Comparator<Point> {
    /**
     * {@link Comparator} that compares {@link Point}s on their <code>x</code>
     * value.
     */
    X {
      @Override
      public int compare(@Nullable Point o1, @Nullable Point o2) {
        return Double.compare(verifyNotNull(o1).x, verifyNotNull(o2).x);
      }
    },
    /**
     * {@link Comparator} that compares {@link Point}s on their <code>y</code>
     * value.
     */
    Y {
      @Override
      public int compare(@Nullable Point o1, @Nullable Point o2) {
        return Double.compare(verifyNotNull(o1).y, verifyNotNull(o2).y);
      }
    },
    /**
     * {@link Comparator} that compares {@link Point}s first on their
     * <code>x</code> value followed by their <code>y</code> value..
     */
    XY {
      @Override
      public int compare(@Nullable Point o1, @Nullable Point o2) {
        return ComparisonChain.start()
          .compare(verifyNotNull(o1).x, verifyNotNull(o2).x)
          .compare(verifyNotNull(o1).y, verifyNotNull(o2).y)
          .result();
      }
    };
  }

  /**
   * Provides default {@link Function}s for transforming {@link Point}s to
   * doubles.
   * @author Rinde van Lon
   */
  public enum Transformers implements Function<Point, Double> {
    /**
     * Transforms {@link Point} to its x value.
     */
    X {
      @Override
      @Nonnull
      public Double apply(@Nullable Point input) {
        return verifyNotNull(input).x;
      }
    },
    /**
     * Transforms {@link Point} to its y value.
     */
    Y {
      @Override
      @Nonnull
      public Double apply(@Nullable Point input) {
        return verifyNotNull(input).y;
      }
    };
  }
}
