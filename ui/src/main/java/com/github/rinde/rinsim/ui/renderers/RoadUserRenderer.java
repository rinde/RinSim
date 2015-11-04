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

import javax.annotation.CheckReturnValue;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractCanvasRenderer;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

/**
 * Renderer that draws simple circles for {@link RoadUser}s in a
 * {@link RoadModel}. Use {@link #builder()} for obtaining instances.
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik changes in handling colors
 */
public final class RoadUserRenderer extends AbstractCanvasRenderer {

  private final RoadModel model;
  private final boolean useEncirclement;
  private final boolean useTextLabel;
  private final UiSchema uiSchema;

  RoadUserRenderer(RoadModel rm, UiSchema schema, boolean encirclement,
      boolean textLabel) {
    model = rm;
    useEncirclement = encirclement;
    useTextLabel = textLabel;
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
                - outerRadius,
              (int) (vp.origin.y + (p.y - vp.rect.min.y)
                  * vp.scale)
                  - outerRadius,
              2 * outerRadius, 2 * outerRadius);
          }
          gc.fillOval((int) (vp.origin.x + (p.x - vp.rect.min.x) * vp.scale)
              - radius,
            (int) (vp.origin.y + (p.y - vp.rect.min.y) * vp.scale)
                - radius,
            2 * radius, 2 * radius);
        }

        if (useTextLabel) {
          gc.drawText(entry.getKey().toString(), x, y, true);
        }

      }
    }
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  /**
   * Constructs a {@link Builder} for building {@link RoadUserRenderer}
   * instances.
   * @return A new builder.
   */
  @CheckReturnValue
  public static Builder builder() {
    return Builder.create();
  }

  /**
   * Builder for {@link RoadUserRenderer}.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder extends
      AbstractModelBuilder<RoadUserRenderer, Void> {

    Builder() {
      setDependencies(RoadModel.class);
    }

    abstract boolean useEncirclement();

    abstract boolean useTextLabel();

    abstract ImmutableMap<Class<?>, RGB> colorMap();

    abstract ImmutableMap<Class<?>, String> imageMap();

    /**
     * Draws a wide circle around all objects.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withCircleAroundObjects() {
      return create(true, useTextLabel(), colorMap(), imageMap());
    }

    @CheckReturnValue
    public Builder withToStringLabel() {
      return create(useEncirclement(), true, colorMap(), imageMap());
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
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withColorAssociation(Class<?> type, RGB rgb) {
      return create(useEncirclement(),
        useTextLabel(),
        ImmutableMap.<Class<?>, RGB>builder()
            .putAll(colorMap())
            .put(type, rgb)
            .build(),
        imageMap());
    }

    /**
     * Associates instances of the specified type with the specified image. The
     * <code>fileName</code> must point to a resource such that it can be loaded
     * using {@link Class#getResourceAsStream(String)}.
     * @param type The class that will be associated with the specified image.
     * @param fileName The file.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withImageAssociation(Class<?> type, String fileName) {
      return create(useEncirclement(), useTextLabel(), colorMap(),
        ImmutableMap.<Class<?>, String>builder()
            .putAll(imageMap())
            .put(type, fileName)
            .build());
    }

    @Override
    public RoadUserRenderer build(DependencyProvider dependencyProvider) {
      final RoadModel rm = dependencyProvider.get(RoadModel.class);

      final UiSchema uis = new UiSchema(colorMap().isEmpty()
          && imageMap().isEmpty());
      for (final Entry<Class<?>, RGB> entry : colorMap().entrySet()) {
        uis.add(entry.getKey(), entry.getValue());
      }
      for (final Entry<Class<?>, String> entry : imageMap().entrySet()) {
        uis.add(entry.getKey(), entry.getValue());
      }
      return new RoadUserRenderer(rm, uis, useEncirclement(), useTextLabel());
    }

    static Builder create() {
      return create(false, false, ImmutableMap.<Class<?>, RGB>of(),
        ImmutableMap.<Class<?>, String>of());
    }

    static Builder create(boolean circle, boolean label,
        ImmutableMap<Class<?>, RGB> colMap,
        ImmutableMap<Class<?>, String> imgMap) {
      return new AutoValue_RoadUserRenderer_Builder(circle, label, colMap,
          imgMap);
    }
  }

}
