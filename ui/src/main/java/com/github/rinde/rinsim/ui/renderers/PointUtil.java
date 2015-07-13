/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.ui.renderers;

import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

final class PointUtil {
  private static final double TOLERANCE = 0.0000001;

  private PointUtil() {}

  static Optional<Point> intersectionPoint(Point own1, Point own2,
      Point oth1, Point oth2) {

    final double dx1 = own2.x - own1.x;
    final double dy1 = own2.y - own1.y;
    final double dx2 = oth2.x - oth1.x;
    final double dy2 = oth2.y - oth1.y;

    final double angle1 = Math.PI + Math.atan2(-dy1, -dx1);
    final double angle2 = Math.PI + Math.atan2(-dy2, -dx2);

    final double sin1 = Math.sin(angle1);
    final double sin2 = Math.sin(angle2);
    final double cos1 = Math.cos(angle1);
    final double cos2 = Math.cos(angle2);

    final double d = sin1 * cos2 - sin2 * cos1;
    if (Math.abs(d) < TOLERANCE) {
      return Optional.absent();
    }
    final double d1 = Math.hypot(dx1, dy1);
    final double d2 = Math.hypot(dx2, dy2);
    final double offset1 = own2.x * own1.y - own1.x * own2.y / d1;
    final double offset2 = oth2.x * oth1.y - oth1.x * oth2.y / d2;

    return Optional.of(new Point(
        (cos1 * offset2 - cos2 * offset1) / d,
        (sin1 * offset2 - sin2 * offset1) / d));
  }

  static double angle(Point p1, Point p2) {
    final double dx = p2.x - p1.x;
    final double dy = p2.y - p1.y;
    return Math.PI + Math.atan2(-dy, -dx);
  }

  static double angle(Connection<?> connection) {
    return angle(connection.from(), connection.to());
  }

  static Point perp(Connection<?> conn, double distOnLine,
      double distFromLine) {
    return PointUtil.perp(conn.from(), conn.to(), distOnLine, distFromLine);
  }

  static Point perp(Point from, Point to, double distOnLine,
      double distFromLine) {
    final Point on = PointUtil.on(from, to, distOnLine);
    final Point unit = PointUtil.unit(from, to);
    return new Point(
        on.x + -unit.y * distFromLine,
        on.y + unit.x * distFromLine);
  }

  static Point on(Connection<?> conn, double dist) {
    return PointUtil.on(conn.from(), conn.to(), dist);
  }

  static Point on(Point from, Point to, double dist) {
    final double length = PointUtil.length(from, to);

    final double ratio = dist / length;
    final double invRatio = 1 - ratio;

    final double x = to.x * ratio + invRatio * from.x;
    final double y = to.y * ratio + invRatio * from.y;
    return new Point(x, y);
  }

  static Point normalize(Point p) {
    return Point.divide(p, PointUtil.length(p));
  }

  static double length(Point p) {
    return Math.sqrt(Math.pow(p.x, 2) + Math.pow(p.y, 2));
  }

  static Point unit(Point from, Point to) {
    return normalize(Point.diff(from, to));
  }

  static Point unit(Connection<?> conn) {
    return unit(conn.from(), conn.to());
  }

  static double length(Point from, Point to) {
    return Point.distance(from, to);
  }

  static double length(Connection<?> conn) {
    return length(conn.from(), conn.to());
  }

  static Point pointInDir(Point value, double angle, double distance) {
    final double x = Math.cos(angle) * distance;
    final double y = Math.sin(angle) * distance;
    return new Point(value.x + x, value.y + y);
  }

  static Point add(Point p1, Point p2) {
    return new Point(p1.x + p2.x, p1.y + p2.y);
  }

  static Point sub(Point point, double value) {
    return new Point(point.x - value, point.y - value);
  }

  static Point add(Point point, double value) {
    return new Point(point.x + value, point.y + value);
  }
}
