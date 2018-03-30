/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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
package com.github.rinde.rinsim.pdptw.common;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Set;

import org.eclipse.swt.graphics.GC;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractCanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/**
 * A renderer that draws the route for any {@link RouteFollowingVehicle}s that
 * exist in the {@link RoadModel}.
 *
 * @author Rinde van Lon
 */
public class RouteRenderer extends AbstractCanvasRenderer {

  RoadModel roadModel;
  PDPModel pdpModel;

  RouteRenderer(RoadModel rm, PDPModel pm) {
    roadModel = rm;
    pdpModel = pm;
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    final Set<RouteFollowingVehicle> vehicles = roadModel.getObjectsOfType(
      RouteFollowingVehicle.class);
    for (final RouteFollowingVehicle v : vehicles) {
      final Set<Parcel> seen = newHashSet();
      final Point from = roadModel.getPosition(v);
      int prevX = vp.toCoordX(from.x);
      int prevY = vp.toCoordY(from.y);

      for (final Parcel parcel : ImmutableList.copyOf(v.getRoute())) {
        final Point to;
        if (pdpModel.getParcelState(parcel).isPickedUp()
          || seen.contains(parcel)) {
          to = parcel.getDto().getDeliveryLocation();
        } else {
          to = parcel.getDto().getPickupLocation();
        }
        seen.add(parcel);
        final int x = vp.toCoordX(to.x);
        final int y = vp.toCoordY(to.y);
        gc.drawLine(prevX, prevY, x, y);

        prevX = x;
        prevY = y;
      }
    }
  }

  /**
   * @return A new {@link Builder} instance.
   */
  public static Builder builder() {
    return new AutoValue_RouteRenderer_Builder();
  }

  /**
   * Builder for {@link RouteRenderer}.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder extends
      AbstractModelBuilder<RouteRenderer, Void> {

    private static final long serialVersionUID = 2467340977162967147L;

    Builder() {
      setDependencies(RoadModel.class, PDPModel.class);
    }

    @Override
    public RouteRenderer build(DependencyProvider dependencyProvider) {
      final RoadModel rm = dependencyProvider.get(RoadModel.class);
      final PDPModel pm = dependencyProvider.get(PDPModel.class);
      return new RouteRenderer(rm, pm);
    }
  }
}
