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

import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;

/**
 * A renderer for a {@link PlaneRoadModel}.
 * @author Rinde van Lon
 */
public final class PlaneRoadModelRenderer implements ModelRenderer {

  private RoadModel rm;
  private final double margin;

  private double xMargin;
  private double yMargin;

  private List<Point> bounds;

  /**
   * @deprecated Use {@link #create()} instead.
   */
  @Deprecated
  public PlaneRoadModelRenderer() {
    this(0.02);
  }

  /**
   * @deprecated Use {@link #create()} instead.
   */
  @Deprecated
  public PlaneRoadModelRenderer(double pMargin) {
    margin = pMargin;
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

    gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    gc.fillRectangle(outerXmin, outerYmin, outerXmax, outerYmax);

    gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
    gc.fillRectangle(xMin, yMin, xMax - xMin, yMax - yMin);
    gc.drawRectangle(xMin, yMin, xMax - xMin, yMax - yMin);
  }

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {}

  @Override
  @Nullable
  public ViewRect getViewRect() {
    return new ViewRect(new Point(bounds.get(0).x - xMargin, bounds.get(0).y
        - yMargin), new Point(bounds.get(1).x + xMargin, bounds.get(1).y
        + yMargin));
  }

  @Override
  public void registerModelProvider(ModelProvider mp) {
    rm = mp.getModel(RoadModel.class);
    bounds = rm.getBounds();
    final double width = bounds.get(1).x - bounds.get(0).x;
    final double height = bounds.get(1).y - bounds.get(0).y;
    xMargin = width * margin;
    yMargin = height * margin;
  }

  /**
   * @return A new {@link PlaneRoadModelRenderer} with a default margin.
   */
  public static PlaneRoadModelRenderer create() {
    return new PlaneRoadModelRenderer();
  }

  /**
   * Creates a new {@link PlaneRoadModelRenderer} instance.
   * @param margin The margin to show around the plane.
   * @return A new instance.
   */
  public static PlaneRoadModelRenderer create(double margin) {
    return new PlaneRoadModelRenderer(margin);
  }
}
