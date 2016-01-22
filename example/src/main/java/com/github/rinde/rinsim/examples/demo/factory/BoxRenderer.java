/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.examples.demo.factory;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Transform;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.VehicleState;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractCanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.UiSchema;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.google.auto.value.AutoValue;

class BoxRenderer extends AbstractCanvasRenderer {
  static final float AT_SITE_ROTATION = 0f;
  static final float IN_CARGO_ROTATION = 20f;
  static final Point LABEL_OFFSET = new Point(-15, -40);
  static final double MAX_PERC = 100d;

  ImageType img;

  enum ImageType {

    SMALL("/graphics/perspective/deliverypackage2.png", new Point(-12, -13),
        new Point(-21, -1)),

    LARGE("/graphics/perspective/deliverypackage3.png", new Point(-20, -21),
        new Point(-23, -8));

    final String file;
    final Point atSiteOffset;
    final Point inCargoOffset;

    ImageType(String f, Point atSite, Point inCargo) {
      file = f;
      atSiteOffset = atSite;
      inCargoOffset = inCargo;
    }

  }

  RoadModel roadModel;
  PDPModel pdpModel;
  final UiSchema uiSchema;

  BoxRenderer(RoadModel rm, PDPModel pm) {
    img = ImageType.LARGE;
    roadModel = rm;
    pdpModel = pm;
    uiSchema = new UiSchema(false);
    uiSchema.add(Box.class, img.file);
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    uiSchema.initialize(gc.getDevice());

    final Collection<Parcel> parcels = pdpModel.getParcels(
        ParcelState.values());
    final Image image = uiSchema.getImage(Box.class);
    checkState(image != null);

    synchronized (pdpModel) {
      final Set<Vehicle> vehicles = pdpModel.getVehicles();
      final Map<Parcel, Vehicle> mapping = new LinkedHashMap<>();
      for (final Vehicle v : vehicles) {
        for (final Parcel p : pdpModel.getContents(v)) {
          mapping.put(p, v);
        }
        if (pdpModel.getVehicleState(v) != VehicleState.IDLE) {
          final PDPModel.VehicleParcelActionInfo vpai = pdpModel
              .getVehicleActionInfo(v);
          mapping.put(vpai.getParcel(), vpai.getVehicle());
        }
      }

      for (final Parcel p : parcels) {
        drawBox(p, gc, vp, time, image, mapping);
      }
    }
  }

  void drawBox(Parcel p, GC gc, ViewPort vp, long time, Image image,
      Map<Parcel, Vehicle> mapping) {
    float rotation = AT_SITE_ROTATION;
    int offsetX = 0;
    int offsetY = 0;
    final ParcelState ps = pdpModel.getParcelState(p);
    if (ps == ParcelState.AVAILABLE) {
      final Point pos = roadModel.getPosition(p);
      final int x = vp.toCoordX(pos.x);
      final int y = vp.toCoordY(pos.y);
      offsetX = (int) img.atSiteOffset.x + x - image.getBounds().width / 2;
      offsetY = (int) img.atSiteOffset.y + y - image.getBounds().height / 2;
    } else
      if (ps == ParcelState.PICKING_UP || ps == ParcelState.DELIVERING) {

      final Vehicle v = mapping.get(p);
      final PDPModel.VehicleParcelActionInfo vpai = pdpModel
          .getVehicleActionInfo(v);
      final Point pos = roadModel.getPosition(v);
      final int x = vp.toCoordX(pos.x);
      final int y = vp.toCoordY(pos.y);
      final double percentage = 1d - vpai.timeNeeded()
          / (double) p.getPickupDuration();
      final String text = (int) (percentage * MAX_PERC) + "%";

      final float rotFac =
          (float) (ps == ParcelState.PICKING_UP ? percentage
              : 1d - percentage);
      rotation = IN_CARGO_ROTATION * rotFac;

      final int textWidth = gc.textExtent(text).x;
      gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_BLUE));
      gc.drawText(text, (int) LABEL_OFFSET.x + x - textWidth / 2,
          (int) LABEL_OFFSET.y + y, true);

      Point from = new Point(img.atSiteOffset.x + x
          - image.getBounds().width
              / 2d,
          img.atSiteOffset.y + y - image.getBounds().height / 2d);
      Point to = new Point(img.inCargoOffset.x + x
          - image.getBounds().width
              / 2d,
          img.inCargoOffset.y + y - image.getBounds().height / 2d);

      if (ps == ParcelState.DELIVERING) {
        final Point temp = from;
        from = to;
        to = temp;
      }

      final Point diff = Point.diff(to, from);
      offsetX = (int) (from.x + percentage * diff.x);
      offsetY = (int) (from.y + percentage * diff.y);

    } else if (ps == ParcelState.IN_CARGO) {
      rotation = IN_CARGO_ROTATION;
      final Point pos = roadModel.getPosition(mapping.get(p));
      final int x = vp.toCoordX(pos.x);
      final int y = vp.toCoordY(pos.y);
      offsetX = (int) img.inCargoOffset.x + x - image.getBounds().width / 2;
      offsetY = (int) img.inCargoOffset.y + y - image.getBounds().height
          / 2;
    }

    if (!ps.isDelivered()) {
      if (rotation == 0f) {
        gc.drawImage(image, offsetX, offsetY);
      } else {
        final Transform oldTransform = new Transform(gc.getDevice());
        gc.getTransform(oldTransform);

        final Transform transform = new Transform(gc.getDevice());
        transform.translate(offsetX + image.getBounds().width / 2, offsetY
            + image.getBounds().height / 2);
        transform.rotate(rotation);
        transform.translate(-(offsetX + image.getBounds().width / 2),
            -(offsetY + image.getBounds().height / 2));
        gc.setTransform(transform);
        gc.drawImage(image, offsetX, offsetY);
        gc.setTransform(oldTransform);
        transform.dispose();
        oldTransform.dispose();
      }
    }
  }

  static Builder builder() {
    return new AutoValue_BoxRenderer_Builder();
  }

  @AutoValue
  abstract static class Builder
      extends AbstractModelBuilder<BoxRenderer, Void> {
    Builder() {
      setDependencies(RoadModel.class, PDPModel.class);
    }

    @Override
    public BoxRenderer build(DependencyProvider dependencyProvider) {
      final RoadModel rm = dependencyProvider.get(RoadModel.class);
      final PDPModel pm = dependencyProvider.get(PDPModel.class);
      return new BoxRenderer(rm, pm);
    }
  }
}
