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

import javax.annotation.CheckReturnValue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractCanvasRenderer;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * A renderer for a {@link PlaneRoadModel}.
 * @author Rinde van Lon
 */
public final class PlaneRoadModelRenderer extends AbstractCanvasRenderer {
  private final double xMargin;
  private final double yMargin;
  private final ImmutableList<Point> bounds;

  PlaneRoadModelRenderer(RoadModel rm, double margin) {
    bounds = rm.getBounds();
    final double width = bounds.get(1).x - bounds.get(0).x;
    final double height = bounds.get(1).y - bounds.get(0).y;
    xMargin = width * margin;
    yMargin = height * margin;
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {
    final int xMin = vp.toCoordX(bounds.get(0).x);
    final int yMin = vp.toCoordY(bounds.get(0).y);
    final int xMax = vp.toCoordX(bounds.get(1).x);
    final int yMax = vp.toCoordY(bounds.get(1).y);

    final int outerXmin = vp.toCoordX(vp.rect.min.x);
    final int outerYmin = vp.toCoordY(vp.rect.min.y);
    final int outerXmax = vp.toCoordX(vp.rect.max.x);
    final int outerYmax = vp.toCoordY(vp.rect.max.y);

    gc.setBackground(
      gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    gc.fillRectangle(outerXmin, outerYmin, outerXmax, outerYmax);

    gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
    gc.fillRectangle(xMin, yMin, xMax - xMin, yMax - yMin);
    gc.drawRectangle(xMin, yMin, xMax - xMin, yMax - yMin);
  }

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {}

  @Override
  public Optional<ViewRect> getViewRect() {
    return Optional.of(new ViewRect(
      new Point(bounds.get(0).x - xMargin, bounds.get(0).y - yMargin),
      new Point(bounds.get(1).x + xMargin, bounds.get(1).y + yMargin)));
  }

  /**
   * @return A {@link Builder} for constructing {@link PlaneRoadModelRenderer}
   *         instances.
   */
  @CheckReturnValue
  public static Builder builder() {
    return Builder.create(Builder.DEFAULT_MARGIN);
  }

  /**
   * Builder for {@link PlaneRoadModelRenderer}.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder extends
      AbstractModelBuilder<PlaneRoadModelRenderer, Void> {
    /**
     * The default margin: 0.02.
     */
    public static final double DEFAULT_MARGIN = 0.02;

    private static final long serialVersionUID = -3124446663942895548L;

    Builder() {
      setDependencies(RoadModel.class);
    }

    abstract double margin();

    /**
     * Set the margin to be drawn around the plane. By default the margin is
     * {@link #DEFAULT_MARGIN}.
     * @param margin The margin.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withMargin(double margin) {
      return create(margin);
    }

    @Override
    public PlaneRoadModelRenderer build(DependencyProvider dependencyProvider) {
      return new PlaneRoadModelRenderer(
        dependencyProvider.get(RoadModel.class), margin());
    }

    static Builder create(double margin) {
      return new AutoValue_PlaneRoadModelRenderer_Builder(margin);
    }
  }
}
