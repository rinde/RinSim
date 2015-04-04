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

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

import com.github.rinde.rinsim.core.ModelProvider;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

/**
 * Renderer that draws simple circles for {@link RoadUser}s in a
 * {@link RoadModel}. Use {@link #builder()} for obtaining instances.
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik changes in handling colors
 */
public final class RoadUserRenderer implements ModelRenderer {

  @Nullable
  private RoadModel model;
  private final boolean useEncirclement;
  private final UiSchema uiSchema;
  private final Optional<ViewRect> viewRect;

  /**
   * @deprecated Use {@link #builder()} instead.
   */
  @Deprecated
  public RoadUserRenderer() {
    this(null, Optional.<ViewRect> absent(), new UiSchema(), false);
  }

  /**
   * @deprecated Use {@link #builder()} instead.
   */
  @Deprecated
  public RoadUserRenderer(UiSchema schema, boolean useEncirclement) {
    this(null, Optional.<ViewRect> absent(), schema, useEncirclement);
  }

  /**
   * @deprecated Use {@link #builder()} instead.
   */
  @Deprecated
  public RoadUserRenderer(@Nullable ViewRect rect, @Nullable UiSchema schema,
      boolean useEncirclement) {
    this(null,
        Optional.fromNullable(rect),
        schema == null ? new UiSchema() : schema,
        useEncirclement);
  }

  RoadUserRenderer(@Nullable RoadModel rm, Optional<ViewRect> rect,
      UiSchema schema, boolean encirclement) {
    model = rm;
    viewRect = rect;
    useEncirclement = encirclement;
    uiSchema = schema;
  }

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    final int radius = 4;
    final int outerRadius = 10;
    uiSchema.initialize(gc.getDevice());
    gc.setBackground(uiSchema.getDefaultColor());

    final Map<RoadUser, Point> objects = model.getObjectsAndPositions();
    synchronized (objects) {
      for (final Entry<RoadUser, Point> entry : objects.entrySet()) {
        final Point p = entry.getValue();
        final Class<?> type = entry.getKey().getClass();
        final Image image = uiSchema.getImage(type);
        final int x = vp.toCoordX(p.x) - radius;
        final int y = vp.toCoordY(p.y) - radius;

        if (image != null) {
          final int offsetX = x - image.getBounds().width / 2;
          final int offsetY = y - image.getBounds().height / 2;
          gc.drawImage(image, offsetX, offsetY);
        } else {
          final Color color = uiSchema.getColor(type);
          if (color == null) {
            continue;
          }
          gc.setBackground(color);
          if (useEncirclement) {
            gc.setForeground(gc.getBackground());
            gc.drawOval((int) (vp.origin.x + (p.x - vp.rect.min.x) * vp.scale)
                - outerRadius, (int) (vp.origin.y + (p.y - vp.rect.min.y)
                * vp.scale)
                - outerRadius, 2 * outerRadius, 2 * outerRadius);
          }
          gc.fillOval((int) (vp.origin.x + (p.x - vp.rect.min.x) * vp.scale)
              - radius, (int) (vp.origin.y + (p.y - vp.rect.min.y) * vp.scale)
              - radius, 2 * radius, 2 * radius);
        }

      }
    }
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  @Nullable
  @Override
  public ViewRect getViewRect() {
    if (viewRect.isPresent()) {
      return viewRect.get();
    }
    return null;
  }

  @Override
  public void registerModelProvider(ModelProvider mp) {
    model = mp.tryGetModel(RoadModel.class);
  }

  /**
   * Constructs a {@link Builder} for building {@link RoadUserRenderer}
   * instances.
   * @return A new builder.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link RoadUserRenderer}.
   * @author Rinde van Lon
   */
  public static class Builder implements CanvasRendererBuilder {
    private boolean useEncirclement;
    private final UiSchema uiSchema;
    private Optional<Point> minPoint;
    private Optional<Point> maxPoint;

    Builder(UiSchema uis) {
      useEncirclement = false;
      uiSchema = uis;
      minPoint = Optional.absent();
      maxPoint = Optional.absent();
    }

    Builder() {
      this(new UiSchema());
    }

    /**
     * Sets the minimum point used to determine the area to draw.
     * @param min The left top corner.
     * @return This, as per the builder pattern.
     */
    public Builder setMinPoint(Point min) {
      minPoint = Optional.of(min);
      return this;
    }

    /**
     * Sets the maximum point used to determine the area to draw.
     * @param max The right bottom corner.
     * @return This, as per the builder pattern.
     */
    public Builder setMaxPoint(Point max) {
      maxPoint = Optional.of(max);
      return this;
    }

    /**
     * Draws a wide circle around all objects.
     * @return This, as per the builder pattern.
     */
    public Builder showCircleAroundObjects() {
      useEncirclement = true;
      return this;
    }

    /**
     * Associate a {@link RGB} to a {@link Class}. This color association works
     * through super classes as well. An example: <br>
     * consider the following class hierarchy<br>
     * <code>class A{}</code><br>
     * <code>class AA extends A{}</code><br>
     * <code>class AAA extends AA{}</code><br>
     * When adding a color named <code>C1</code> to <code>AA</code>, both
     * <code>AA</code> and <code>AAA</code> will have color <code>C1</code>.
     * When adding another color named <code>C2</code> to <code>A</code>
     * <code>A</code> will have color <code>C2</code> and <code>AA</code> and
     * <code>AAA</code> will have color <code>C1</code>.
     * @param type The {@link Class} used as identifier.
     * @param rgb The {@link RGB} instance used as color.
     * @return This, as per the builder pattern.
     */
    public Builder addColorAssociation(Class<?> type, RGB rgb) {
      uiSchema.add(type, rgb);
      return this;
    }

    /**
     * Associates instances of the specified type with the specified image. The
     * <code>fileName</code> must point to a resource such that it can be loaded
     * using {@link Class#getResourceAsStream(String)}.
     * @param type The class that will be associated with the specified image.
     * @param fileName The file.
     * @return This, as per the builder pattern.
     */
    public Builder addImageAssociation(Class<?> type, String fileName) {
      uiSchema.add(type, fileName);
      return this;
    }

    @Override
    public CanvasRenderer build(ModelProvider mp) {
      Optional<ViewRect> viewRect = Optional.absent();
      if (minPoint.isPresent() && maxPoint.isPresent()) {
        viewRect = Optional.of(new ViewRect(minPoint.get(), maxPoint.get()));
      }
      final RoadModel rm = mp.getModel(RoadModel.class);

      return new RoadUserRenderer(rm, viewRect, uiSchema, useEncirclement);
    }

    @Override
    public CanvasRendererBuilder copy() {
      final Builder copy = new Builder(uiSchema);
      copy.useEncirclement = useEncirclement;
      copy.minPoint = minPoint;
      copy.maxPoint = maxPoint;
      return copy;
    }
  }
}
