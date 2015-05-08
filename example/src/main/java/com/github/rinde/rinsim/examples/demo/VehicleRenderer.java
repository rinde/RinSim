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
package com.github.rinde.rinsim.examples.demo;

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.github.rinde.rinsim.ui.renderers.ViewRect;
import com.google.auto.value.AutoValue;

final class VehicleRenderer extends AbstractModel<Void> implements
  CanvasRenderer {
  private final RoadModel rm;

  VehicleRenderer(RoadModel r) {
    rm = r;
  }

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    final int radius = 2;
    gc.setBackground(new Color(gc.getDevice(), 255, 0, 0));
    final Map<RoadUser, Point> objects = rm.getObjectsAndPositions();
    synchronized (objects) {
      for (final Entry<RoadUser, Point> entry : objects.entrySet()) {
        final Point p = entry.getValue();
        gc.fillOval((int) (vp.origin.x + (p.x - vp.rect.min.x) * vp.scale)
          - radius, (int) (vp.origin.y + (p.y - vp.rect.min.y) * vp.scale)
          - radius, 2 * radius, 2 * radius);
      }
    }
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  @Nullable
  @Override
  public ViewRect getViewRect() {
    return null;
  }

  @Override
  public boolean register(Void element) {
    return false;
  }

  @Override
  public boolean unregister(Void element) {
    return false;
  }

  static Builder builder() {
    return new AutoValue_VehicleRenderer_Builder();
  }

  @AutoValue
  abstract static class Builder extends
    AbstractModelBuilder<VehicleRenderer, Void> {
    Builder() {
      setDependencies(RoadModel.class);
    }

    @Override
    public VehicleRenderer build(DependencyProvider dependencyProvider) {
      final RoadModel rm = dependencyProvider.get(RoadModel.class);
      return new VehicleRenderer(rm);
    }
  }
}
