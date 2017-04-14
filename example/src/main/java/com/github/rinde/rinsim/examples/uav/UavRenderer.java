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

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.road.CollisionPlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractCanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.google.auto.value.AutoValue;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;

import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Hoang Tung Dinh
 */
final class UavRenderer extends AbstractCanvasRenderer {
  private static final int RED = 255;
  private final CollisionPlaneRoadModel rm;

  UavRenderer(CollisionPlaneRoadModel r) {
    rm = r;
  }

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    final int radius = 2;
    final Map<RoadUser, Point> objects = rm.getObjectsAndPositions();
    synchronized (objects) {
      for (final Entry<RoadUser, Point> entry : objects.entrySet()) {
        final Point p = entry.getValue();
        gc.setBackground(new Color(gc.getDevice(), 0, 0, 255));
        gc.fillOval(
            (int) (vp.origin.x + (p.x - vp.rect.min.x) * vp.scale) - (int) (rm.getObjectRadius()
                * vp.scale),
            (int) (vp.origin.y + (p.y - vp.rect.min.y) * vp.scale) - (int) (rm.getObjectRadius()
                * vp.scale),
            2 * (int) (rm.getObjectRadius() * vp.scale),
            2 * (int) (rm.getObjectRadius() * vp.scale));

        gc.setBackground(new Color(gc.getDevice(), RED, 0, 0));
        gc.fillOval((int) (vp.origin.x + (p.x - vp.rect.min.x) * vp.scale) - radius,
            (int) (vp.origin.y + (p.y - vp.rect.min.y) * vp.scale) - radius, 2 * radius,
            2 * radius);
      }
    }
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  static Builder builder() {
    return new AutoValue_UavRenderer_Builder();
  }

  @AutoValue
  abstract static class Builder extends AbstractModelBuilder<UavRenderer, Void> {
    private static final long serialVersionUID = -5497962300581400051L;

    Builder() {
      setDependencies(CollisionPlaneRoadModel.class);
    }

    @Override
    public UavRenderer build(DependencyProvider dependencyProvider) {
      return new UavRenderer(dependencyProvider.get(CollisionPlaneRoadModel.class));
    }
  }
}
