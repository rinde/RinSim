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
package com.github.rinde.rinsim.examples.pdptw.gradientfield;

import static com.google.common.base.Verify.verifyNotNull;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;

import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.ModelRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.github.rinde.rinsim.ui.renderers.ViewRect;

class GradientFieldRenderer implements ModelRenderer {

  final static RGB GREEN = new RGB(0, 255, 0);
  final static RGB RED = new RGB(255, 0, 0);

  @Nullable
  GradientModel gradientModel;

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    final List<Truck> trucks = verifyNotNull(gradientModel).getTruckEmitters();

    synchronized (trucks) {
      for (final Truck t : trucks) {
        final Point tp = t.getPosition();
        final Map<Point, Float> fields = t.getFields();

        float max = Float.NEGATIVE_INFINITY;
        float min = Float.POSITIVE_INFINITY;

        for (final Point p : fields.keySet()) {
          max = Math.max(max, fields.get(p));
          min = Math.min(min, fields.get(p));
        }
        int dia;
        RGB color = null;
        for (final Point p : fields.keySet()) {
          final int x = vp.toCoordX(tp.x + p.x / 6d);
          final int y = vp.toCoordY(tp.y + p.y / 6d);
          final float field = fields.get(p);

          if (field < 0) {
            dia = (int) (field / -min * 6);
            color = RED;
          } else {
            dia = (int) (field / max * 6);
            color = GREEN;
          }
          gc.setBackground(new Color(gc.getDevice(), color));
          gc.fillOval(x, y, dia, dia);
        }
      }
    }
  }

  @Nullable
  @Override
  public ViewRect getViewRect() {
    return null;
  }

  @Override
  public void registerModelProvider(ModelProvider mp) {
    gradientModel = mp.tryGetModel(GradientModel.class);
  }
}
