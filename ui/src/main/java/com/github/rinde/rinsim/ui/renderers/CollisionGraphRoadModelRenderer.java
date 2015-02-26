/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.math3.geometry.euclidean.twod.Line;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Path;

import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Table;

/**
 * @author Rinde van Lon
 *
 */
public class CollisionGraphRoadModelRenderer implements ModelRenderer {

  Optional<CollisionGraphRoadModel> model;
  private final int margin = 2;
  private static final int NODE_RADIUS = 2;
  private static final Point RELATIVE_TEXT_POSITION = new Point(4, -14);

  /**
   *
   */
  public CollisionGraphRoadModelRenderer() {
    model = Optional.absent();
  }

  @Override
  public void registerModelProvider(ModelProvider mp) {
    model = Optional.fromNullable(mp.getModel(CollisionGraphRoadModel.class));
  }

  static class AdaptedGC {
    private final GC gc;
    private final ViewPort vp;

    AdaptedGC(GC g, ViewPort v) {
      gc = g;
      vp = v;
    }

    void drawLine(Point p1, Point p2) {
      gc.drawLine(
          vp.toCoordX(p1.x), vp.toCoordY(p1.y),
          vp.toCoordX(p2.x), vp.toCoordY(p2.y));
    }

    void setForegroundSysCol(int next) {
      gc.setForeground(gc.getDevice().getSystemColor(next));
    }

    void setBackgroundSysCol(int next) {
      gc.setBackground(gc.getDevice().getSystemColor(next));
    }

    void drawCurve(Point p1, Point p2, Point control) {
      final Path path = new Path(gc.getDevice());
      path.moveTo(vp.toCoordX(p1.x), vp.toCoordY(p1.y));
      path.quadTo(
          vp.toCoordX(control.x), vp.toCoordY(control.y),
          vp.toCoordX(p2.x), vp.toCoordY(p2.y));
      gc.drawPath(path);
      path.dispose();
    }

    void drawCircle(Point p, double radius) {
      gc.drawOval(vp.toCoordX(p.x - radius), vp.toCoordY(p.y - radius),
          vp.toCoordX(radius * 2d), vp.toCoordY(radius * 2d));
    }

    void drawCircle(Point p, int radius) {
      gc.drawOval(vp.toCoordX(p.x - radius), vp.toCoordY(p.y - radius),
          radius * 2, radius * 2);
    }
  }

  static double length(Connection<?> conn) {
    return length(conn.from(), conn.to());
  }

  static double length(Point from, Point to) {
    return Point.distance(from, to);
  }

  static Point unit(Connection<?> conn) {
    return unit(conn.from(), conn.to());
  }

  static Point unit(Point from, Point to) {
    return normalize(Point.diff(from, to));
  }

  static double length(Point p) {
    return Math.sqrt(Math.pow(p.x, 2) + Math.pow(p.y, 2));
  }

  static Point normalize(Point p) {
    return Point.divide(p, length(p));
  }

  static Point on(Point from, Point to, double dist) {
    final double length = length(from, to);

    final double ratio = dist / length;
    final double invRatio = 1 - ratio;

    final double x = to.x * ratio + invRatio * from.x;
    final double y = to.y * ratio + invRatio * from.y;
    return new Point(x, y);
  }

  static Point on(Connection<?> conn, double dist) {
    return on(conn.from(), conn.to(), dist);
  }

  static Point perp(Point from, Point to, double distOnLine, double distFromLine) {
    final Point on = on(from, to, distOnLine);
    final Point unit = unit(from, to);
    return new Point(
        on.x + -unit.y * distFromLine,
        on.y + unit.x * distFromLine);
  }

  static Point perp(Connection<?> conn, double distOnLine, double distFromLine) {
    return perp(conn.from(), conn.to(), distOnLine, distFromLine);
  }

  static Optional<Point> intersectionPoint(Point own1, Point own2,
      Point other1,
      Point other2) {

    final Line line1 = new Line(new Vector2D(own1.x, own1.y), new Vector2D(
        own2.x,
        own2.y), 0.00001);
    final Line line2 = new Line(new Vector2D(other1.x, other1.y), new Vector2D(
        other2.x,
        other2.y), 0.00001);

    final Vector2D intersect = line1.intersection(line2);
    if (intersect == null) {
      return Optional.absent();
    }

    return Optional.of(new Point(intersect.getX(), intersect.getY()));

    // final Point dxyOwn = Point.diff(own2, own1);
    // final Point dxyOth = Point.diff(other2, other1);
    //
    // final double mOwn;
    // if (dxyOwn.x == 0) {
    // mOwn = 1;
    // } else if (dxyOwn.y == 0) {
    // mOwn = 0;
    // } else {
    // mOwn = dxyOwn.y / dxyOwn.x;
    // }
    //
    // final double mOth;
    // if (dxyOth.x == 0) {
    // mOth = 1;
    // } else if (dxyOth.y == 0) {
    // mOth = 0;
    // } else {
    // mOth = dxyOth.y / dxyOth.x;
    // }
    //
    // System.out.println(mOwn + " " + mOth);
    // if (Math.abs(mOwn - mOth) < .0000001) {
    // return Optional.absent();
    // }
    //
    // final double cOwn = own1.y - mOwn * own1.x;
    // final double cOth = other1.y - mOth * other1.y;
    //
    // final double x = (cOth - cOwn) / (mOwn - mOth);
    // final double y = mOwn * x + cOwn;
    // return Optional.of(new Point(x, y));

    // final double A2 = other2.y - other1.y;
    // final double B2 = other2.x - other1.x;
    // final double C2 = A2 * other1.x + B2 * other1.y;
    //
    // final double A1 = own2.y - own1.y;
    // final double B1 = own2.x - own1.x;
    // final double C1 = A1 * own1.x + B1 * own1.y;
    //
    // final double det = A1 * B2 - A2 * B1;
    // if (Math.abs(det) < .0000001) {
    // return Optional.absent();
    // }
    // final Point d = new Point((B2 * C1 - B1 * C2) / det, -(A1 * C2 - A2 * C1)
    // / det);
    // return Optional.of(d);

  }

  // static Point rotate(Point p, double angle) {
  // return new Point(p.x * Math.cos(angle) - p.y * Math.sin(angle),
  // p.x * Math.sin(angle) + p.y * Math.cos(angle));
  // }

  static double angle(Point p1, Point p2) {
    final double dx = p1.x - p2.x;
    final double dy = p1.y - p2.y;
    return Math.atan2(dy, dx);
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {

    final Graph<? extends ConnectionData> graph = model.get().getGraph();

    final AdaptedGC adapter = new AdaptedGC(gc, vp);

    // if (showNodes || showNodeLabels) {
    // for (final Point node : graph.getNodes()) {
    // final int x1 = vp.toCoordX(node.x) - NODE_RADIUS;
    // final int y1 = vp.toCoordY(node.y) - NODE_RADIUS;
    //
    // // if (showNodes) {
    // final int size = NODE_RADIUS * 2;
    // gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
    // gc.fillOval(x1, y1, size, size);
    // // }
    // // if (showNodeLabels) {
    // // gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
    // // gc.drawString(node.toString(), x1 + (int) RELATIVE_TEXT_POSITION.x,
    // // y1 + (int) RELATIVE_TEXT_POSITION.y, true);
    // // }
    // // }
    // }

    final double vehicleLength = model.get().getVehicleLength();
    final double roadWidth = model.get().getVehicleLength() / 2d;
    final double halfRoadWidth = roadWidth / 2d;

    // filter connections to avoid double work for bidirectional roads

    final Table<Point, Point, Connection<?>> filteredConnections = HashBasedTable
        .create();

    for (final Connection<?> e : graph.getConnections()) {
      if (!filteredConnections.contains(e.to(), e.from())) {
        filteredConnections.put(e.from(), e.to(), e);
      }
    }

    for (final Connection<?> e : filteredConnections.values()) {
      gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
      final double length = length(e);

      final Point a = perp(e, vehicleLength, halfRoadWidth);
      final Point b = perp(e, length - vehicleLength, halfRoadWidth);
      adapter.drawLine(a, b);
      final Point c = perp(e, vehicleLength, -halfRoadWidth);
      final Point d = perp(e, length - vehicleLength, -halfRoadWidth);
      adapter.drawLine(c, d);
    }

    for (final Point p : graph.getNodes()) {
      final Set<Point> conns = new LinkedHashSet<>();
      conns.addAll(graph.getIncomingConnections(p));
      conns.addAll(graph.getOutgoingConnections(p));

      final List<Point> neighbors = new ArrayList<>(conns);
      Collections.sort(neighbors, new Comparator<Point>() {
        @Override
        public int compare(@Nullable Point o1, @Nullable Point o2) {
          assert o1 != null;
          assert o2 != null;
          return Double.compare(angle(p, o1), angle(p, o2));
        }
      });

      final Iterator<Integer> colors = Iterators.cycle(SWT.COLOR_BLUE,
          SWT.COLOR_GREEN, SWT.COLOR_RED, SWT.COLOR_CYAN, SWT.COLOR_MAGENTA);

      System.out.println(neighbors);

      neighbors.add(neighbors.get(0));
      final PeekingIterator<Point> it = Iterators.peekingIterator(neighbors
          .iterator());

      for (Point n = it.next(); it.hasNext(); n = it.next()) {
        if (!it.hasNext()) {
          break;
        }
        adapter.setForegroundSysCol(colors.next());

        // adapter.drawLine(p, on(p, n, 1));
        // adapter.drawLine(perp(p, n, 1, .5), perp(p, n, 2, .5));

        final Point a = perp(p, n, vehicleLength, -halfRoadWidth);
        final Point a2 = perp(p, n, vehicleLength - 1, -halfRoadWidth);
        final Point b = perp(p, it.peek(), vehicleLength, halfRoadWidth);
        final Point b2 = perp(p, it.peek(), vehicleLength - 1, halfRoadWidth);
        final Optional<Point> intersect = intersectionPoint(a, a2, b, b2);

        // adapter.drawLine(a, a2);
        // adapter.drawLine(b, b2);

        if (intersect.isPresent()) {
          final Point control = intersect.get();// perp(p, n, halfRoadWidth,
                                                // halfRoadWidth);
                                                // adapter.drawCircle(control,
                                                // 2);
          adapter.setForegroundSysCol(SWT.COLOR_GRAY);
          adapter.drawCurve(a, b, control);
        }
        else {
          adapter.setForegroundSysCol(SWT.COLOR_GRAY);
          adapter.drawLine(a, b);
        }

        // final Path path = new Path(gc.getDevice());
        // path.moveTo(vp.toCoordX(a.x), vp.toCoordY(a.y));
        // path.quadTo(vp.toCoordX(p.x), vp.toCoordY(p.y),
        // vp.toCoordX(b.x), vp.toCoordY(b.y));
        // gc.drawPath(path);
        // path.dispose();

        // adapter.drawLine(perp(p, n, vehicleLength, -halfRoadWidth),
        // perp(p, it.peek(), vehicleLength, halfRoadWidth));

      }
    }

  }

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {}

  @Nullable
  @Override
  public ViewRect getViewRect() {
    final Graph<?> graph = model.get().getGraph();
    checkState(!model.get().getGraph().isEmpty(),
        "graph may not be empty at this point");
    final Collection<Point> nodes = model.get().getGraph().getNodes();

    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;

    for (final Point p : nodes) {
      minX = Math.min(minX, p.x);
      maxX = Math.max(maxX, p.x);
      minY = Math.min(minY, p.y);
      maxY = Math.max(maxY, p.y);
    }
    return new ViewRect(new Point(minX - margin, minY - margin), new Point(maxX
        + margin, maxY + margin));
  }

}
