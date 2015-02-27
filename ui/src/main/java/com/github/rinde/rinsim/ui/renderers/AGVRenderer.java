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

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.inferred.freebuilder.FreeBuilder;

import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.Factory;
import com.github.rinde.rinsim.ui.IBuilder;
import com.github.rinde.rinsim.ui.renderers.AGVRenderer.AGVRendererFactory.Builder;
import com.google.common.base.Optional;

/**
 * @author Rinde van Lon
 *
 */
public final class AGVRenderer implements CanvasRenderer {

  CollisionGraphRoadModel model;
  RenderHelper helper;

  AGVRenderer(CollisionGraphRoadModel m, AGVRendererFactory factory) {
    model = m;
    helper = new RenderHelper();
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    helper.adapt(gc, vp);
    final Map<RoadUser, Point> obs = model.getObjectsAndPositions();
    for (final Entry<RoadUser, Point> entry : obs.entrySet()) {

      final Optional<? extends Connection<?>> conn = model.getConnection(entry
          .getKey());

      if (conn.isPresent()) {
        final double angle = PointUtil.angle(conn.get());
        final Point back = PointUtil
            .pointInDir(entry.getValue(), angle, -model.getVehicleLength());

        helper.setBackgroundSysCol(SWT.COLOR_RED);
        helper.drawArrow(back, entry.getValue(), model.getVehicleLength() / 2d,
            model.getVehicleLength() / 2d);

        helper.setBackgroundSysCol(SWT.COLOR_BLUE);
      }
      else {
        helper.fillCircle(entry.getValue(), model.getVehicleLength() / 4d);
      }
    }
  }

  @Override
  @Nullable
  public ViewRect getViewRect() {
    return null;
  }

  public static Builder builder() {
    return new Builder();
  }

  @FreeBuilder
  public abstract static class AGVRendererFactory implements
      Factory<AGVRenderer, ModelProvider> {
    AGVRendererFactory() {}

    @Override
    public AGVRenderer create(ModelProvider argument) {
      return new AGVRenderer(argument.getModel(CollisionGraphRoadModel.class),
          this);
    }

    public static class Builder extends AGVRenderer_AGVRendererFactory_Builder
        implements IBuilder<Factory<AGVRenderer, ModelProvider>> {}

  }
}
