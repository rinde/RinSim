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

import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.swt.graphics.GC;

import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

public class TestRenderer implements ModelRenderer {

  Optional<RoadModel> roadModel;

  @Override
  public void registerModelProvider(ModelProvider mp) {
    roadModel = Optional.fromNullable(mp.tryGetModel(RoadModel.class));
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    final List<Point> bounds = roadModel.get().getBounds();

    gc.drawLine(vp.toCoordX(bounds.get(0).x), vp.toCoordY(bounds.get(0).y),
      vp.toCoordX(bounds.get(1).x), vp.toCoordY(bounds.get(1).y));

    gc.drawText("fancy pancy", vp.toCoordX(bounds.get(0).x),
      vp.toCoordY(bounds.get(0).y) + 100, true);

  }

  @Override
  @Nullable
  public ViewRect getViewRect() {
    return null;
  }

}
