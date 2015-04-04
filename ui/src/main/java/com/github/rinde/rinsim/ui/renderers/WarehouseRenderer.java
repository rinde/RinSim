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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import com.github.rinde.rinsim.core.ModelProvider;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.Point;
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
  /**
   * The definition of a 'short' connection. Connections that are smaller or
   * equal to this number times the length of the vehicle are considered short.
   */
  private static final double SHORT_CONNECTION_LENGTH = 3d;

  /**
   * The dimensions of the head of the arrow relative to the vehicle length.
   */
  private static final Point ARROW_HEAD_REL_DIM = new Point(.25, .25);

  private static final int OPAQUE = 255;
  private static final int SEMI_TRANSPARENT = 50;

  private final CollisionGraphRoadModel model;
  private final double margin;
  private final RenderHelper adapter;
  private final double vehicleLength;
  private final double minDistance;
  private final double halfRoadWidth;
  private final Graph<?> graph;
  private final boolean drawOneWayStreetArrows;
  private final boolean showNodeOccupancy;
  private final boolean showNodes;
  private final Point arrowDimensions;

  WarehouseRenderer(Builder builder, CollisionGraphRoadModel m) {
    model = m;
    graph = model.getGraph();
    margin = builder.margin + m.getVehicleLength() / 2d;
    drawOneWayStreetArrows = builder.drawOneWayStreetArrows;
    showNodeOccupancy = builder.showNodeOccupancy;
    showNodes = builder.showNodes;
    adapter = new RenderHelper();
    vehicleLength = model.getVehicleLength();
    minDistance = model.getMinDistance();
    final double roadWidth = model.getVehicleLength();
    halfRoadWidth = roadWidth / 2d;
    arrowDimensions = new Point(
        ARROW_HEAD_REL_DIM.x * roadWidth,
        ARROW_HEAD_REL_DIM.y * roadWidth);
  }

  private Table<Point, Point, Connection<?>> filterConnections() {
    // filter connections to avoid double work for bidirectional roads
    final Table<Point, Point, Connection<?>> filteredConnections = HashBasedTable
        .create();
    for (final Connection<?> e : graph.getConnections()) {
      if (!filteredConnections.contains(e.to(), e.from())) {
        filteredConnections.put(e.from(), e.to(), e);
      }
    }
    return filteredConnections;
  }

  private void drawConnections() {
    // filter connections to avoid double work for bidirectional roads
    final Table<Point, Point, Connection<?>> filteredConnections = filterConnections();

    adapter.setForegroundSysCol(SWT.COLOR_DARK_GRAY);
    adapter.setBackgroundSysCol(SWT.COLOR_DARK_GRAY);
    // draw connections
    for (final Connection<?> e : filteredConnections.values()) {
      // draw arrows
      if (!graph.hasConnection(e.to(), e.from()) && drawOneWayStreetArrows) {
        if (PointUtil.length(e) > SHORT_CONNECTION_LENGTH * vehicleLength) {
          final Point start1 = PointUtil.on(e, vehicleLength);
          final Point end1 = PointUtil.on(e,
              vehicleLength + arrowDimensions.y * 2);
          adapter.drawArrow(start1, end1, arrowDimensions.x, arrowDimensions.y);

          final Point start2 = PointUtil.on(e.to(), e.from(),
              vehicleLength + arrowDimensions.y * 2);
          final Point end2 = PointUtil.on(e.to(), e.from(), vehicleLength);
          adapter.drawArrow(start2, end2, arrowDimensions.x, arrowDimensions.y);
        } else {
          final double center = PointUtil.length(e) / 2d;
          final Point start1 = PointUtil.on(e, center - arrowDimensions.y);
          final Point end1 = PointUtil.on(e, center + arrowDimensions.y);
          adapter.drawArrow(start1, end1, arrowDimensions.x, arrowDimensions.y);
        }
      }

      adapter.setForegroundSysCol(SWT.COLOR_GRAY);
      adapter.setBackgroundSysCol(SWT.COLOR_GRAY);
      final double length = PointUtil.length(e);

      final Point a = PointUtil.perp(e, vehicleLength, halfRoadWidth);
      final Point b = PointUtil.perp(e, length - vehicleLength, halfRoadWidth);
      adapter.drawLine(a, b);
      final Point c = PointUtil.perp(e, vehicleLength, -halfRoadWidth);
      final Point d = PointUtil.perp(e, length - vehicleLength, -halfRoadWidth);
      adapter.drawLine(c, d);
    }
  }

  private void drawNodes() {
    // draw node connectors
    for (final Point p : graph.getNodes()) {
      if (showNodes) {
        adapter.setBackgroundSysCol(SWT.COLOR_RED);
        adapter.fillCircle(p, 2);
      }

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
  public void renderStatic(GC gc, ViewPort vp) {
    adapter.adapt(gc, vp);
    drawConnections();
    drawNodes();
  }

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    adapter.adapt(gc, vp);
    if (showNodeOccupancy) {
      for (final Point p : model.getOccupiedNodes()) {
        gc.setAlpha(SEMI_TRANSPARENT);
        adapter.setBackgroundSysCol(SWT.COLOR_RED);
        adapter.fillCircle(p, vehicleLength / 2d + minDistance);
        gc.setAlpha(OPAQUE);
      }
    }
  }

  @Nullable
  @Override
  public ViewRect getViewRect() {
    checkState(!graph.isEmpty(),
        "graph may not be empty at this point");
    final List<Point> extremes = Graphs.getExtremes(graph);
    return new ViewRect(
        PointUtil.sub(extremes.get(0), margin),
        PointUtil.add(extremes.get(1), margin));
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
    boolean showNodes;

    Builder() {
      margin = 0;
      drawOneWayStreetArrows = false;
      showNodeOccupancy = false;
      showNodes = false;
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

    /**
     * Will draw a small dot for each node. By default this is not shown.
     * @return This, as per the builder pattern.
     */
    public Builder showNodes() {
      showNodes = true;
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
      b.showNodes = showNodes;
      return b;
    }
  }
}
