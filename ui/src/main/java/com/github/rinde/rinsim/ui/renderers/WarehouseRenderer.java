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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.CanvasRendererBuilder;
import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Table;

/**
 * Renders a graph as an warehouse. Instances can be obtained via
 * {@link #builder()}.
 * <p>
 * <b>Requires:</b> a {@link CollisionGraphRoadModel} in the
 * {@link com.github.rinde.rinsim.core.Simulator}.
 * @author Rinde van Lon
 */
public final class WarehouseRenderer implements CanvasRenderer {
  private final CollisionGraphRoadModel model;
  private final double margin;
  private final RenderHelper adapter;
  private final double vehicleLength;
  private final double roadWidth;
  private final double halfRoadWidth;
  private final Graph<?> graph;
  private final boolean drawOneWayStreetArrows;
  private final boolean showNodeOccupancy;

  WarehouseRenderer(Builder builder, CollisionGraphRoadModel m) {
    model = m;
    graph = model.getGraph();
    margin = builder.margin + m.getVehicleLength() / 2d;
    drawOneWayStreetArrows = builder.drawOneWayStreetArrows;
    showNodeOccupancy = builder.showNodeOccupancy;
    adapter = new RenderHelper();
    vehicleLength = model.getVehicleLength();
    roadWidth = model.getVehicleLength() / 1.5d;
    halfRoadWidth = roadWidth / 2d;
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {
    adapter.adapt(gc, vp);

    // filter connections to avoid double work for bidirectional roads
    final Table<Point, Point, Connection<?>> filteredConnections = HashBasedTable
        .create();
    for (final Connection<?> e : graph.getConnections()) {
      if (!filteredConnections.contains(e.to(), e.from())) {
        filteredConnections.put(e.from(), e.to(), e);
      }
    }

    adapter.setForegroundSysCol(SWT.COLOR_GRAY);
    adapter.setBackgroundSysCol(SWT.COLOR_GRAY);
    // draw connections
    for (final Connection<?> e : filteredConnections.values()) {
      // draw arrows
      if (!graph.hasConnection(e.to(), e.from()) && drawOneWayStreetArrows) {
        if (PointUtil.length(e) > 3 * vehicleLength) {
          final Point start1 = PointUtil.on(e, 1 * vehicleLength);
          final Point end1 = PointUtil.on(e, 1.5 * vehicleLength);
          adapter.drawArrow(start1, end1, roadWidth / 2d, vehicleLength / 4d);
          final Point start2 = PointUtil.on(e.to(), e.from(),
              1.5 * vehicleLength);
          final Point end2 = PointUtil.on(e.to(), e.from(), 1 * vehicleLength);
          adapter.drawArrow(start2, end2, roadWidth / 2d, vehicleLength / 4d);

        } else {
          final double center = PointUtil.length(e) / 2d;
          final Point start1 = PointUtil.on(e, center - vehicleLength / 4d);
          final Point end1 = PointUtil.on(e, center + vehicleLength / 4d);
          adapter.drawArrow(start1, end1, roadWidth / 2d, vehicleLength / 4d);
        }
      }

      final double length = PointUtil.length(e);

      final Point a = PointUtil.perp(e, vehicleLength, halfRoadWidth);
      final Point b = PointUtil.perp(e, length - vehicleLength, halfRoadWidth);
      adapter.drawLine(a, b);
      final Point c = PointUtil.perp(e, vehicleLength, -halfRoadWidth);
      final Point d = PointUtil.perp(e, length - vehicleLength, -halfRoadWidth);
      adapter.drawLine(c, d);
    }

    // draw node connectors
    for (final Point p : graph.getNodes()) {
      final Set<Point> conns = new LinkedHashSet<>();
      conns.addAll(graph.getIncomingConnections(p));
      conns.addAll(graph.getOutgoingConnections(p));

      if (conns.size() == 1) {
        // dead end is a special case
        final Point n = conns.iterator().next();
        final Point c1 = PointUtil.perp(p, n, -vehicleLength, -halfRoadWidth);
        final Point c2 = PointUtil.perp(p, n, -vehicleLength, halfRoadWidth);
        final Point o1 = PointUtil.perp(p, n, vehicleLength, -halfRoadWidth);
        final Point o2 = PointUtil.perp(p, n, vehicleLength, halfRoadWidth);
        adapter.setForegroundSysCol(SWT.COLOR_GRAY);
        adapter.drawPolyline(o1, c1, c2, o2);
      } else {
        final List<Point> neighbors = new ArrayList<>(conns);
        Collections.sort(neighbors, new Comparator<Point>() {
          @Override
          public int compare(@Nullable Point o1, @Nullable Point o2) {
            assert o1 != null;
            assert o2 != null;
            return Double.compare(PointUtil.angle(p, o1),
                PointUtil.angle(p, o2));
          }
        });

        neighbors.add(neighbors.get(0));
        final PeekingIterator<Point> it = Iterators.peekingIterator(neighbors
            .iterator());

        for (Point n = it.next(); it.hasNext(); n = it.next()) {
          if (!it.hasNext()) {
            break;
          }
          final Point a = PointUtil.perp(p, n, vehicleLength, -halfRoadWidth);
          final Point a2 = PointUtil
              .perp(p, n, vehicleLength + 1, -halfRoadWidth);
          final Point b = PointUtil.perp(p, it.peek(), vehicleLength,
              halfRoadWidth);
          final Point b2 = PointUtil.perp(p, it.peek(), vehicleLength + 1,
              halfRoadWidth);
          final Optional<Point> intersect = PointUtil.intersectionPoint(a, a2,
              b,
              b2);

          if (intersect.isPresent()) {
            final Point control = intersect.get();
            adapter.setForegroundSysCol(SWT.COLOR_GRAY);
            adapter.drawCurve(a, b, control);
          } else {
            adapter.setForegroundSysCol(SWT.COLOR_GRAY);
            adapter.drawLine(a, b);
          }
        }
      }
    }
  }

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    adapter.adapt(gc, vp);
    if (showNodeOccupancy) {
      for (final Point p : model.getOccupiedNodes()) {
        gc.setAlpha(50);
        adapter.setBackgroundSysCol(SWT.COLOR_RED);
        adapter.fillCircle(p, vehicleLength);
        gc.setAlpha(255);
      }
    }
  }

  @Nullable
  @Override
  public ViewRect getViewRect() {
    checkState(!graph.isEmpty(),
        "graph may not be empty at this point");
    final Collection<Point> nodes = graph.getNodes();

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

  /**
   * @return A new {@link Builder} for creating a {@link WarehouseRenderer}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for creating a {@link WarehouseRenderer}.
   * @author Rinde van Lon
   */
  public static class Builder implements CanvasRendererBuilder {
    boolean drawOneWayStreetArrows;
    double margin;
    boolean showNodeOccupancy;

    Builder() {
      margin = 0;
      drawOneWayStreetArrows = false;
      showNodeOccupancy = false;
    }

    /**
     * Defines the margin around the warehouse. The margin is defined in the
     * unit used by the {@link CollisionGraphRoadModel}. The default value is
     * <code>0</code>.
     * @param m Must be a positive value.
     * @return This, as per the builder pattern.
     */
    public Builder setMargin(double m) {
      checkArgument(m >= 0d);
      margin = m;
      return this;
    }

    /**
     * One way streets will be indicated with an arrow indicating the allowed
     * driving direction. By default this is not drawn.
     * @return This, as per the builder pattern.
     */
    public Builder drawOneWayStreetArrows() {
      drawOneWayStreetArrows = true;
      return this;
    }

    /**
     * Will draw an overlay on occupied nodes. By default this is not shown.
     * @return This, as per the builder pattern.
     */
    public Builder showNodeOccupancy() {
      showNodeOccupancy = true;
      return this;
    }

    @Override
    public CanvasRenderer build(ModelProvider mp) {
      return new WarehouseRenderer(this,
          mp.getModel(CollisionGraphRoadModel.class));
    }

    @Override
    public CanvasRendererBuilder copy() {
      final Builder b = new Builder();
      b.drawOneWayStreetArrows = drawOneWayStreetArrows;
      b.margin = margin;
      b.showNodeOccupancy = showNodeOccupancy;
      return b;
    }
  }
}
