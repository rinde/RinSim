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

import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.comm.CommUser;

/**
 * @author Rinde van Lon
 *
 */
public final class CommRenderer implements CanvasRenderer {

  private final CommModel model;
  private final RenderHelper helper;

  CommRenderer(Builder b, CommModel cm) {
    model = cm;
    helper = new RenderHelper();
  }

  // make color classes based on reliability
  // interpolate between two extremes? e.g. between red and green

  // show received message count?
  // show send message count?
  // give objects ids, unique colors, etc
  // fix random problem

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    helper.adapt(gc, vp);
    for (final Entry<CommUser, CommDevice> entry : model.getUsersAndDevices()
        .entrySet()) {

      final CommUser user = entry.getKey();
      final CommDevice device = entry.getValue();

      if (device.getMaxRange().isPresent()) {
        helper.drawCircle(user.getPosition(), device.getMaxRange().get());

        helper.setBackgroundSysCol(SWT.COLOR_DARK_BLUE);
        gc.setAlpha(50);
        helper.fillCircle(user.getPosition(), device.getMaxRange().get());
      }
      gc.setAlpha(255);
      helper.setBackgroundSysCol(SWT.COLOR_RED);
      helper.fillCircle(user.getPosition(), .05);

    }
  }

  @Nullable
  @Override
  public ViewRect getViewRect() {
    return null;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder implements CanvasRendererBuilder {
    @Override
    public CanvasRenderer build(ModelProvider mp) {
      return new CommRenderer(this, mp.getModel(CommModel.class));
    }

    @Override
    public CanvasRendererBuilder copy() {
      return new Builder();
    }
  }
}
