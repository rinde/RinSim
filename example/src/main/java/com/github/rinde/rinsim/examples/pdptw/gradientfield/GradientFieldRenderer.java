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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractCanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.google.auto.value.AutoValue;

class GradientFieldRenderer extends AbstractCanvasRenderer {

  static final double DIAMETER_MUL = 6d;
  static final RGB GREEN = new RGB(0, 255, 0);
  static final RGB RED = new RGB(255, 0, 0);

  GradientModel gradientModel;

  GradientFieldRenderer(GradientModel gm) {
    gradientModel = gm;
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    final List<Truck> trucks = gradientModel.getTruckEmitters();

    synchronized (trucks) {
      for (final Truck t : trucks) {
        final Point tp = t.getPosition();

        final Map<Point, Float> fields = t.getFields();

        float max = Float.NEGATIVE_INFINITY;
        float min = Float.POSITIVE_INFINITY;

        for (final Entry<Point, Float> p : fields.entrySet()) {
          max = Math.max(max, p.getValue());
          min = Math.min(min, p.getValue());
        }
        int dia;
        RGB color = null;
        for (final Entry<Point, Float> entry : fields.entrySet()) {
          final Point p = entry.getKey();
          final float field = entry.getValue();
          final int x = vp.toCoordX(tp.x + p.x / 6d);
          final int y = vp.toCoordY(tp.y + p.y / 6d);

          if (field < 0) {
            dia = (int) (field / -min * DIAMETER_MUL);
            color = RED;
          } else {
            dia = (int) (field / max * DIAMETER_MUL);
            color = GREEN;
          }
          gc.setBackground(new Color(gc.getDevice(), color));
          gc.fillOval(x, y, dia, dia);
        }
      }
    }
  }

  static Builder builder() {
    return new AutoValue_GradientFieldRenderer_Builder();
  }

  @AutoValue
  abstract static class Builder extends
      AbstractModelBuilder<GradientFieldRenderer, Void> {

    Builder() {
      setDependencies(GradientModel.class);
    }

    @Override
    public GradientFieldRenderer build(DependencyProvider dependencyProvider) {
      final GradientModel gm = dependencyProvider.get(GradientModel.class);
      return new GradientFieldRenderer(gm);
    }
  }
}
