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

import java.util.Collection;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.CanvasRendererBuilder;

/**
 * A simple {@link CanvasRenderer} for {@link GraphRoadModel}s. Instances can be
 * obtained via {@link #builder()}.
 * <p>
 * <b>Requires:</b> a {@link GraphRoadModel} in the
 * {@link com.github.rinde.rinsim.core.Simulator}.
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
public final class GraphRoadModelRenderer implements CanvasRenderer {
  private static final int NODE_RADIUS = 2;
  private static final Point RELATIVE_TEXT_POSITION = new Point(4, -14);

  private final GraphRoadModel model;
  private final int margin;
  private final boolean showNodes;
  private final boolean showNodeLabels;
  private final boolean showDirectionArrows;

  GraphRoadModelRenderer(GraphRoadModel grm, Builder b) {
    model = grm;
    margin = b.margin;
    showNodes = b.showNodes;
    showNodeLabels = b.showNodeLabels;
    showDirectionArrows = b.showDirectionArrows;
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {
    final Graph<? extends ConnectionData> graph = model.getGraph();

    if (showNodes || showNodeLabels) {
      for (final Point node : graph.getNodes()) {
        final int x1 = vp.toCoordX(node.x) - NODE_RADIUS;
        final int y1 = vp.toCoordY(node.y) - NODE_RADIUS;

        if (showNodes) {
          final int size = NODE_RADIUS * 2;
          gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
          gc.fillOval(x1, y1, size, size);
        }
        if (showNodeLabels) {
          gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
          gc.drawString(node.toString(), x1 + (int) RELATIVE_TEXT_POSITION.x,
              y1 + (int) RELATIVE_TEXT_POSITION.y, true);
        }
      }
    }

    for (final Connection<? extends ConnectionData> e : graph.getConnections()) {
      final int x1 = vp.toCoordX(e.from().x);
      final int y1 = vp.toCoordY(e.from().y);

      final int x2 = vp.toCoordX(e.to().x);
      final int y2 = vp.toCoordY(e.to().y);
      gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
      gc.drawLine(x1, y1, x2, y2);

      if (showDirectionArrows) {
        final double dist = Point
            .distance(new Point(x1, y1), new Point(x2, y2));
        final double r = 14d / dist;
        final double r2 = 4d / dist;
        final Point unit = Point.divide(
            Point.diff(new Point(x1, y1), new Point(x2, y2)), dist);

        // get two points on the line
        final int x3 = (int) (r * x1 + (1 - r) * x2);
        final int y3 = (int) (r * y1 + (1 - r) * y2);
        final int x6 = (int) (r2 * x1 + (1 - r2) * x2);
        final int y6 = (int) (r2 * y1 + (1 - r2) * y2);

        // get two points perpendicular to the line next to point 3
        final int x4 = (int) (x3 + 5 * unit.y);
        final int y4 = (int) (y3 + 5 * unit.x);
        final int x5 = (int) (x3 - 5 * unit.y);
        final int y5 = (int) (y3 - 5 * unit.x);

        // draw the arrow
        gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
        gc.fillPolygon(new int[] { x4, y4, x5, y5, x6, y6 });
      }
    }
  }

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {}

  @Nullable
  @Override
  public ViewRect getViewRect() {
    checkState(!model.getGraph().isEmpty(),
        "graph may not be empty at this point");
    final Collection<Point> nodes = model.getGraph().getNodes();

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
   * @return A new {@link Builder} for creating {@link GraphRoadModelRenderer}
   *         instances.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for creating a {@link GraphRoadModelRenderer}.
   * @author Rinde van Lon
   */
  public static final class Builder implements CanvasRendererBuilder {
    int margin;
    boolean showNodes;
    boolean showNodeLabels;
    boolean showDirectionArrows;

    Builder() {
      margin = 0;
      showNodes = false;
      showNodeLabels = false;
      showDirectionArrows = false;
    }

    /**
     * Sets the margin to display around the graph.
     * @param m The margin, in the same unit as
     *          {@link GraphRoadModel#getDistanceUnit()}.
     * @return This, as per the builder pattern.
     */
    public Builder setMargin(int m) {
      checkArgument(m >= 0);
      margin = m;
      return this;
    }

    /**
     * Draws a circle for each node in the graph.
     * @return This, as per the builder pattern.
     */
    public Builder showNodes() {
      showNodes = true;
      return this;
    }

    /**
     * Shows a label with coordinates next to each node in the graph.
     * @return This, as per the builder pattern.
     */
    public Builder showNodeLabels() {
      showNodeLabels = true;
      return this;
    }

    /**
     * Shows arrows for each connection in the graph, indicating the allowed
     * driving direction(s).
     * @return This, as per the builder pattern.
     */
    public Builder showDirectionArrows() {
      showDirectionArrows = true;
      return this;
    }

    @Override
    public Builder copy() {
      final Builder copy = new Builder();
      copy.margin = margin;
      copy.showNodes = showNodes;
      copy.showNodeLabels = showNodeLabels;
      copy.showDirectionArrows = showDirectionArrows;
      return copy;
    }

    @Override
    public CanvasRenderer build(ModelProvider mp) {
      return new GraphRoadModelRenderer(mp.getModel(GraphRoadModel.class), this);
    }
  }
}
