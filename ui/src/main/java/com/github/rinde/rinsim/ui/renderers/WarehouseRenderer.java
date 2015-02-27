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
import org.inferred.freebuilder.FreeBuilder;

import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.Factory;
import com.github.rinde.rinsim.ui.IBuilder;
import com.github.rinde.rinsim.ui.renderers.WarehouseRenderer.WarehouseRendererFactory.Builder;
import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Table;

/**
 * @author Rinde van Lon
 *
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

  WarehouseRenderer(WarehouseRendererFactory factory, CollisionGraphRoadModel m) {
    model = m;
    graph = model.getGraph();
    margin = factory.getMargin() + m.getVehicleLength() / 2d;
    drawOneWayStreetArrows = factory.isDrawOneWayStreetArrows();
    adapter = new RenderHelper();
    vehicleLength = model.getVehicleLength();
    roadWidth = model.getVehicleLength() / 2d;
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

      final List<Point> neighbors = new ArrayList<>(conns);
      Collections.sort(neighbors, new Comparator<Point>() {
        @Override
        public int compare(@Nullable Point o1, @Nullable Point o2) {
          assert o1 != null;
          assert o2 != null;
          return Double.compare(PointUtil.angle(p, o1), PointUtil.angle(p, o2));
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
        final Optional<Point> intersect = PointUtil.intersectionPoint(a, a2, b,
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

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {}

  @Nullable
  @Override
  public ViewRect getViewRect() {
    final Graph<?> graph = model.getGraph();
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

  public static Builder builder() {
    return new Builder();
  }

  @FreeBuilder
  public abstract static class WarehouseRendererFactory implements
      Factory<WarehouseRenderer, ModelProvider> {

    WarehouseRendererFactory() {}

    /**
     * @return
     */
    abstract boolean isDrawOneWayStreetArrows();

    /**
     * Margin around the warehouse in the unit used by the
     * {@link CollisionGraphRoadModel}..
     * @return The margin.
     */
    abstract double getMargin();

    @Override
    public WarehouseRenderer create(ModelProvider argument) {
      return new WarehouseRenderer(this,
          argument.getModel(CollisionGraphRoadModel.class));
    }

    public static class Builder extends
        WarehouseRenderer_WarehouseRendererFactory_Builder implements
        IBuilder<WarehouseRendererFactory> {

      Builder() {
        setMargin(0d);
        setDrawOneWayStreetArrows(false);
      }

      /**
       * {@inheritDoc} Must be a positive value.
       */
      @Override
      public Builder setMargin(double margin) {
        checkArgument(margin >= 0d);
        return super.setMargin(margin);
      }
    }
  }

}
