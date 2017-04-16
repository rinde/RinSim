/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.examples.uav;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.road.CollisionPlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractCanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.google.auto.value.AutoValue;

/**
 * @author Hoang Tung Dinh
 */
final class UavRenderer extends AbstractCanvasRenderer {
  private static final int RED = 255;
  private final CollisionPlaneRoadModel rm;

  private final Color red;
  private final Color blue;
  private final Color black;

  UavRenderer(CollisionPlaneRoadModel r, Device d) {
    rm = r;
    red = d.getSystemColor(SWT.COLOR_RED);
    blue = d.getSystemColor(SWT.COLOR_BLUE);
    black = d.getSystemColor(SWT.COLOR_BLACK);
  }

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    final int radius = 2;
    final Map<RoadUser, Point> objects = rm.getObjectsAndPositions();
    synchronized (objects) {
      for (final Entry<RoadUser, Point> entry : objects.entrySet()) {
        final Point p = entry.getValue();
        final double r = rm.getObjectRadius();

        final int xpx = vp.toCoordX(p.x);
        final int ypx = vp.toCoordY(p.y);
        gc.setBackground(blue);
        gc.fillOval(
          xpx - vp.scale(r), ypx - vp.scale(r),
          2 * vp.scale(r), 2 * vp.scale(r));

        gc.setForeground(blue);
        gc.drawOval(
          xpx - 2 * vp.scale(r), ypx - 2 * vp.scale(r),
          4 * vp.scale(r), 4 * vp.scale(r));

        gc.setBackground(red);
        gc.fillOval(
          xpx - radius,
          ypx - radius,
          2 * radius,
          2 * radius);

        final Point dest = ((UavAgent) entry.getKey()).destination.orNull();
        if (dest != null) {
          gc.setForeground(red);
          gc.drawLine(xpx, ypx, vp.toCoordX(dest.x), vp.toCoordY(dest.y));
        }
        gc.setForeground(black);
        gc.drawText(entry.getKey().toString(), xpx, ypx);

      }
    }
  }

  static Point pointInDir(Point value, double angle, double distance) {
    final double x = Math.cos(angle) * distance;
    final double y = Math.sin(angle) * distance;
    return new Point(value.x + x, value.y + y);
  }

  static double angle(Point p1, Point p2) {
    final double dx = p2.x - p1.x;
    final double dy = p2.y - p1.y;
    return Math.PI + Math.atan2(-dy, -dx);
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  static Builder builder() {
    return new AutoValue_UavRenderer_Builder();
  }

  @AutoValue
  abstract static class Builder
      extends AbstractModelBuilder<UavRenderer, Void> {
    private static final long serialVersionUID = -5497962300581400051L;

    Builder() {
      setDependencies(CollisionPlaneRoadModel.class, Device.class);
    }

    @Override
    public UavRenderer build(DependencyProvider dependencyProvider) {
      return new UavRenderer(
        dependencyProvider.get(CollisionPlaneRoadModel.class),
        dependencyProvider.get(Device.class));
    }
  }
}
