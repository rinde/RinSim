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
package com.github.rinde.rinsim.ui.renderers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import javax.annotation.CheckReturnValue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractCanvasRenderer;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * A simple {@link CanvasRenderer} for {@link GraphRoadModel}s. Instances can be
 * obtained via {@link #builder()}.
 * <p>
 * <b>Requires:</b> a {@link GraphRoadModel} in the
 * {@link com.github.rinde.rinsim.core.Simulator}.
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
public final class GraphRoadModelRenderer extends AbstractCanvasRenderer {
  private static final int NODE_RADIUS = 2;
  private static final Point RELATIVE_TEXT_POSITION = new Point(4, -14);
  private static final int ARROW_HEAD_SIZE = 8;
  private static final Point ARROW_REL_FROM_TO = new Point(.9, .95);

  private final GraphRoadModel model;
  private final int margin;
  private final boolean showNodes;
  private final boolean showNodeCoordinates;
  private final boolean showDirectionArrows;
  private final RenderHelper helper;

  GraphRoadModelRenderer(GraphRoadModel grm, Builder b) {
    model = grm;
    margin = b.margin();
    showNodes = b.vizOptions().contains(VizOptions.NODE_CIRCLES);
    showNodeCoordinates = b.vizOptions().contains(VizOptions.NODE_COORDS);
    showDirectionArrows = b.vizOptions().contains(VizOptions.DIR_ARROWS);
    helper = new RenderHelper();
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {
    helper.adapt(gc, vp);
    final Graph<? extends ConnectionData> graph = model.getGraph();

    if (showNodes) {
      for (final Point node : graph.getNodes()) {
        helper.setBackgroundSysCol(SWT.COLOR_RED);
        helper.fillCircle(node, NODE_RADIUS);
      }
    }
    if (showNodeCoordinates) {
      for (final Point node : graph.getNodes()) {
        helper.setForegroundSysCol(SWT.COLOR_GRAY);
        helper.drawString(node.toString(), node, true,
          (int) RELATIVE_TEXT_POSITION.x, (int) RELATIVE_TEXT_POSITION.y);
      }
    }

    for (final Connection<? extends ConnectionData> e : graph
      .getConnections()) {
      helper.setForegroundSysCol(SWT.COLOR_GRAY);
      helper.drawLine(e.from(), e.to());

      if (showDirectionArrows) {
        final double dist = Point.distance(e.from(), e.to());
        final Point f = PointUtil.on(e, dist * ARROW_REL_FROM_TO.x);
        final Point t = PointUtil.on(e, dist * ARROW_REL_FROM_TO.y);
        helper.setBackgroundSysCol(SWT.COLOR_GRAY);
        helper.drawArrow(f, t, ARROW_HEAD_SIZE, ARROW_HEAD_SIZE);
      }
    }
  }

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {}

  @Override
  public Optional<ViewRect> getViewRect() {
    checkState(!model.getGraph().isEmpty(),
      "graph may not be empty at this point");

    final List<Point> extremes = Graphs.getExtremes(model.getGraph());
    return Optional.of(new ViewRect(
      PointUtil.sub(extremes.get(0), margin),
      PointUtil.add(extremes.get(1), margin)));
  }

  /**
   * @return A new {@link Builder} for creating {@link GraphRoadModelRenderer}
   *         instances.
   */
  @CheckReturnValue
  public static Builder builder() {
    return Builder.create();
  }

  enum VizOptions {
    NODE_CIRCLES, NODE_COORDS, DIR_ARROWS;
  }

  /**
   * A builder for creating a {@link GraphRoadModelRenderer}.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder extends
      AbstractModelBuilder<GraphRoadModelRenderer, Void> {

    Builder() {
      setDependencies(GraphRoadModel.class);
    }

    abstract int margin();

    abstract ImmutableSet<VizOptions> vizOptions();

    /**
     * Sets the margin to display around the graph.
     * @param m The margin, in the same unit as
     *          {@link GraphRoadModel#getDistanceUnit()}.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withMargin(int m) {
      checkArgument(m >= 0);
      return create(m, vizOptions());
    }

    /**
     * Draws a circle for each node in the graph.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withNodeCircles() {
      return create(margin(), VizOptions.NODE_CIRCLES, vizOptions());
    }

    /**
     * Shows a label with coordinates next to each node in the graph.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withNodeCoordinates() {
      return create(margin(), VizOptions.NODE_COORDS, vizOptions());
    }

    /**
     * Shows arrows for each connection in the graph, indicating the allowed
     * driving direction(s).
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withDirectionArrows() {
      return create(margin(), VizOptions.DIR_ARROWS, vizOptions());
    }

    @Override
    public GraphRoadModelRenderer build(DependencyProvider dependencyProvider) {
      return new GraphRoadModelRenderer(
        dependencyProvider.get(GraphRoadModel.class), this);
    }

    static Builder create() {
      return create(0, ImmutableSet.<VizOptions>of());
    }

    static Builder create(int margin, ImmutableSet<VizOptions> opts) {
      return new AutoValue_GraphRoadModelRenderer_Builder(margin, opts);
    }

    static Builder create(int margin, VizOptions opt,
        ImmutableSet<VizOptions> opts) {
      return create(margin,
        Sets.immutableEnumSet(opt, opts.toArray(new VizOptions[] {})));
    }
  }
}
