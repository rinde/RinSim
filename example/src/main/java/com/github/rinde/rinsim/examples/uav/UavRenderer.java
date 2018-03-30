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
package com.github.rinde.rinsim.examples.uav;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.road.CollisionPlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractCanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;

/**
 *
 * @author Hoang Tung Dinh
 * @author Rinde van Lon
 */
final class UavRenderer extends AbstractCanvasRenderer {
  private static final int FONT_SIZE = 10;

  private final CollisionPlaneRoadModel rm;
  private final Color red;
  private final Color black;
  private final Color darkGray;
  private final Font labelFont;
  private final ImmutableSet<Opts> vizOptions;
  private final Iterator<Integer> colors = Iterators.cycle(SWT.COLOR_BLUE,
    SWT.COLOR_RED, SWT.COLOR_GREEN, SWT.COLOR_CYAN, SWT.COLOR_MAGENTA,
    SWT.COLOR_YELLOW, SWT.COLOR_DARK_BLUE, SWT.COLOR_DARK_RED,
    SWT.COLOR_DARK_GREEN, SWT.COLOR_DARK_CYAN, SWT.COLOR_DARK_MAGENTA,
    SWT.COLOR_DARK_YELLOW);

  private final Map<RoadUser, Color> colorMap;

  UavRenderer(CollisionPlaneRoadModel r, Device d, ImmutableSet<Opts> opts) {
    rm = r;
    red = d.getSystemColor(SWT.COLOR_RED);
    black = d.getSystemColor(SWT.COLOR_BLACK);
    darkGray = d.getSystemColor(SWT.COLOR_GRAY);
    labelFont = new Font(d, "arial", FONT_SIZE, SWT.NORMAL);
    vizOptions = opts;
    colorMap = new LinkedHashMap<>();
  }

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    final int radius = 2;
    final Map<RoadUser, Point> objects = rm.getObjectsAndPositions();
    synchronized (objects) {
      for (final Entry<RoadUser, Point> entry : objects.entrySet()) {
        final Point p = entry.getValue();
        final UavAgent a = (UavAgent) entry.getKey();
        final double r = rm.getObjectRadius();
        final Color c;
        if (vizOptions.contains(Opts.DIFFERENT_COLORS)) {
          if (!colorMap.containsKey(a)) {
            colorMap.put(a, gc.getDevice().getSystemColor(colors.next()));
          }
          c = colorMap.get(a);
        } else {
          c = red;
        }

        final int xpx = vp.toCoordX(p.x);
        final int ypx = vp.toCoordY(p.y);

        if (vizOptions.contains(Opts.DESTINATION)) {
          final Point dest = a.getDestination().orNull();
          if (dest != null) {
            gc.setForeground(c);
            gc.setLineStyle(SWT.LINE_DOT);
            gc.drawLine(vp.toCoordX(dest.x), vp.toCoordY(dest.y), xpx, ypx);
            gc.setLineStyle(SWT.LINE_SOLID);
          }
        }

        gc.setBackground(darkGray);
        gc.fillOval(
          xpx - vp.scale(r), ypx - vp.scale(r),
          2 * vp.scale(r), 2 * vp.scale(r));

        gc.setForeground(c);
        gc.drawOval(
          xpx - vp.scale(r), ypx - vp.scale(r),
          2 * vp.scale(r), 2 * vp.scale(r));

        if (vizOptions.contains(Opts.NAME)) {
          gc.setForeground(black);
          gc.setFont(labelFont);

          final org.eclipse.swt.graphics.Point stringExtent =
            gc.stringExtent(a.getName());
          gc.drawText(a.getName(), xpx - stringExtent.x / 2,
            ypx - stringExtent.y / 2, true);

        } else {
          gc.setBackground(c);
          gc.fillOval(
            xpx - radius,
            ypx - radius,
            2 * radius,
            2 * radius);
        }
      }
    }
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  static Builder builder() {
    return Builder.create();
  }

  enum Opts {
    DESTINATION, NAME, DIFFERENT_COLORS;
  }

  @AutoValue
  abstract static class Builder
      extends AbstractModelBuilder<UavRenderer, Void> {
    private static final long serialVersionUID = 701437750634453331L;

    Builder() {
      setDependencies(CollisionPlaneRoadModel.class, Device.class);
    }

    abstract ImmutableSet<Opts> vizOptions();

    public Builder withDestinationLines() {
      return create(Opts.DESTINATION, vizOptions());
    }

    public Builder withName() {
      return create(Opts.NAME, vizOptions());
    }

    public Builder withDifferentColors() {
      return create(Opts.DIFFERENT_COLORS, vizOptions());
    }

    @Override
    public UavRenderer build(DependencyProvider dependencyProvider) {
      return new UavRenderer(
        dependencyProvider.get(CollisionPlaneRoadModel.class),
        dependencyProvider.get(Device.class),
        vizOptions());
    }

    static Builder create(Opts opt, ImmutableSet<Opts> opts) {
      return new AutoValue_UavRenderer_Builder(
        ImmutableSet.<Opts>builder().addAll(opts).add(opt).build());
    }

    static Builder create() {
      return new AutoValue_UavRenderer_Builder(ImmutableSet.<Opts>of());
    }
  }
}
