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
package com.github.rinde.rinsim.ui.renderers;

import static com.github.rinde.rinsim.ui.renderers.PointUtil.angle;
import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckReturnValue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Transform;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractTypedCanvasRenderer;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

/**
 * Renders vehicles as AGVs. Instances can be obtained via
 * {@link AGVRenderer#builder()}.
 * <p>
 * <b>Requires:</b> a {@link CollisionGraphRoadModel} in the
 * {@link com.github.rinde.rinsim.core.Simulator}.
 * @author Rinde van Lon
 */
public final class AGVRenderer
    extends AbstractTypedCanvasRenderer<MovingRoadUser> {
  private static final int DEFAULT_COLOR = SWT.COLOR_BLACK;
  private final CollisionGraphRoadModel model;
  private final RenderHelper helper;
  private final Map<MovingRoadUser, VehicleUI> vehicles;
  private final ImmutableSet<VizOptions> vizOptions;
  private int vehicleCounter;

  private final Iterator<Integer> colors = Iterators.cycle(SWT.COLOR_BLUE,
    SWT.COLOR_RED, SWT.COLOR_GREEN, SWT.COLOR_CYAN, SWT.COLOR_MAGENTA,
    SWT.COLOR_YELLOW, SWT.COLOR_DARK_BLUE, SWT.COLOR_DARK_RED,
    SWT.COLOR_DARK_GREEN, SWT.COLOR_DARK_CYAN, SWT.COLOR_DARK_MAGENTA,
    SWT.COLOR_DARK_YELLOW);

  enum VizOptions {
    COORDINATES, CREATION_NUMBER, VEHICLE_ORIGIN, USE_DIFFERENT_COLORS;
  }

  AGVRenderer(CollisionGraphRoadModel m, ImmutableSet<VizOptions> options) {
    model = m;
    helper = new RenderHelper();
    vehicles = Collections
      .synchronizedMap(new LinkedHashMap<MovingRoadUser, VehicleUI>());
    vehicleCounter = 0;
    vizOptions = options;
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    helper.adapt(gc, vp);
    synchronized (vehicles) {
      final Map<RoadUser, Point> objMap = model.getObjectsAndPositions();
      for (final VehicleUI v : vehicles.values()) {
        v.update(gc, vp, helper, objMap);
      }
    }
  }

  @Override
  public boolean register(MovingRoadUser mru) {
    final int color = vizOptions.contains(VizOptions.USE_DIFFERENT_COLORS)
      ? colors.next() : DEFAULT_COLOR;
    final VehicleUI v = new VehicleUI(mru, model, color, vizOptions,
      vehicleCounter++);

    verify(vehicles.put(mru, v) == null);
    return true;
  }

  @Override
  public boolean unregister(MovingRoadUser mru) {
    verifyNotNull(vehicles.remove(mru)).dispose();
    return true;
  }

  /**
   * @return A {@link Builder} for creating an {@link AGVRenderer}.
   */
  @CheckReturnValue
  public static Builder builder() {
    return Builder.create();
  }

  /**
   * A builder for creating {@link AGVRenderer}s.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder
      extends AbstractModelBuilder<AGVRenderer, MovingRoadUser> {
    private static final long serialVersionUID = -8359744710512375486L;

    Builder() {
      setDependencies(CollisionGraphRoadModel.class);
    }

    abstract ImmutableSet<VizOptions> vizOptions();

    /**
     * Draws a number on each vehicle. The number indicates the creation order
     * of the vehicle.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withVehicleCreationNumber() {
      return create(VizOptions.CREATION_NUMBER, vizOptions());
    }

    /**
     * Displays the coordinates of each vehicle next to it.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withVehicleCoordinates() {
      return create(VizOptions.COORDINATES, vizOptions());
    }

    /**
     * Vehicles are drawn with different colors to ease debugging.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withDifferentColorsForVehicles() {
      return create(VizOptions.USE_DIFFERENT_COLORS, vizOptions());
    }

    /**
     * Vehicles are drawn with a small half circle on top, the center of this
     * half circle indicates the vehicle origin. The origin is the actual
     * position as returned by
     * {@link CollisionGraphRoadModelImpl#getPosition(RoadUser)}.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withVehicleOrigin() {
      return create(VizOptions.VEHICLE_ORIGIN, vizOptions());
    }

    @Override
    public AGVRenderer build(DependencyProvider dp) {
      final CollisionGraphRoadModel rm =
        dp.get(CollisionGraphRoadModel.class);
      return new AGVRenderer(rm, vizOptions());
    }

    static Builder create() {
      return new AutoValue_AGVRenderer_Builder(
        Sets.immutableEnumSet(ImmutableSet.<VizOptions>of()));
    }

    static Builder create(VizOptions one, Iterable<VizOptions> more) {
      return new AutoValue_AGVRenderer_Builder(Sets.immutableEnumSet(one,
        Iterables.toArray(more, VizOptions.class)));
    }
  }

  static class VehicleUI {
    static final int DOT_SIZE_PX = 4;
    static final int ROTATION_OFFSET_DEG = 90;
    static final int ROTATION_MAX_DEG = 180;

    final CollisionGraphRoadModel model;
    final MovingRoadUser vehicle;
    Point position;
    double angle;
    final int color;
    final Set<VizOptions> vizOptions;
    final int creationNumber;
    double scale = 1;
    Optional<Image> image;

    VehicleUI(MovingRoadUser mru, CollisionGraphRoadModel m, int c,
        Set<VizOptions> t, int num) {
      vehicle = mru;
      model = m;
      angle = 0;
      color = c;
      vizOptions = t;
      creationNumber = num;
      position = new Point(0, 0);
      image = Optional.absent();
    }

    void dispose() {
      if (image.isPresent()) {
        image.get().dispose();
      }
    }

    Image createImage(GC gc, ViewPort vp) {
      if (image.isPresent()) {
        image.get().dispose();
      }
      final int length = (int) (model.getVehicleLength() * vp.scale);
      final int width = length / 2;
      final int frontSize = length / 8;
      final Image img = new Image(gc.getDevice(), width, length);
      final GC igc = new GC(img);

      igc.setBackground(gc.getDevice().getSystemColor(color));
      igc.fillPolygon(new int[] {
        frontSize, 0,
        width - frontSize, 0,
        width, frontSize,
        0, frontSize
      });
      igc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
      igc.fillRectangle(0, frontSize, width, length - frontSize);

      if (vizOptions.contains(VizOptions.CREATION_NUMBER)) {
        final String string = Integer.toString(creationNumber);
        final double factor = width / (double) igc.stringExtent(string).x;
        final Font initialFont = igc.getFont();
        final FontData[] fontData = initialFont.getFontData();
        for (int i = 0; i < fontData.length; i++) {
          fontData[i].setHeight((int) (fontData[i].getHeight() * factor));
        }
        final Font newFont = new Font(gc.getDevice(), fontData);
        igc.setFont(newFont);

        final org.eclipse.swt.graphics.Point finalTextSize = igc
          .stringExtent(string);

        final int xOffset = (int) ((width - finalTextSize.x) / 2d);
        final int yOffset = frontSize
          + (int) ((length - frontSize - finalTextSize.y) / 2d);
        igc.drawText(string, xOffset, yOffset, true);
        newFont.dispose();
      }

      if (vizOptions.contains(VizOptions.VEHICLE_ORIGIN)) {
        igc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
        igc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
        igc.fillOval(width / 2 - 2, length / 2 - 2, DOT_SIZE_PX, DOT_SIZE_PX);
        igc.drawOval(width / 2 - 2, length / 2 - 2, DOT_SIZE_PX, DOT_SIZE_PX);
      }

      igc.dispose();
      return img;
    }

    void update(GC gc, ViewPort vp, RenderHelper helper,
        Map<RoadUser, Point> objMap) {
      if (!objMap.containsKey(vehicle)) {
        return;
      }
      position = objMap.get(vehicle);

      final Optional<? extends Connection<?>> conn =
        model.getConnection(vehicle);

      if (!image.isPresent() || scale != vp.scale) {
        scale = vp.scale;
        image = Optional.of(createImage(gc, vp));
      }

      if (conn.isPresent()) {
        angle = angle(conn.get());
      }

      final int x = vp.toCoordX(position.x);
      final int y = vp.toCoordY(position.y);

      final Transform transform = new Transform(gc.getDevice());
      transform.translate(x, y);
      transform.rotate(
        (float) (ROTATION_OFFSET_DEG + angle * ROTATION_MAX_DEG / Math.PI));
      transform.translate(
        -(x + image.get().getBounds().width / 2),
        -(y + image.get().getBounds().height / 2));
      gc.setTransform(transform);
      gc.drawImage(image.get(), x, y);
      gc.setTransform(null);
      transform.dispose();

      if (vizOptions.contains(VizOptions.COORDINATES)) {
        helper.setBackgroundSysCol(SWT.COLOR_YELLOW);
        helper.setForegroundSysCol(SWT.COLOR_BLACK);
        gc.drawString(String.format("%1.2f,%1.2f", position.x, position.y),
          vp.toCoordX(position.x),
          vp.toCoordY(position.y));
      }
    }
  }
}
