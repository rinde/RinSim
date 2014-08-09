package com.github.rinde.rinsim.examples.demo.factory;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newLinkedHashMap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Transform;

import com.github.rinde.rinsim.core.graph.Point;
import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.VehicleState;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.ui.renderers.ModelRenderer;
import com.github.rinde.rinsim.ui.renderers.UiSchema;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.github.rinde.rinsim.ui.renderers.ViewRect;
import com.google.common.base.Optional;

class BoxRenderer implements ModelRenderer {

  // static final Point AT_SITE_OFFSET = new Point(-12, -13);
  static final float AT_SITE_ROTATION = 0f;
  // static final Point IN_CARGO_OFFSET = new Point(-21, -1);
  static final float IN_CARGO_ROTATION = 20f;
  static final Point LABEL_OFFSET = new Point(-15, -40);

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

  Optional<RoadModel> roadModel;
  Optional<PDPModel> pdpModel;
  final UiSchema uiSchema;

  BoxRenderer() {
    img = ImageType.LARGE;
    roadModel = Optional.absent();
    pdpModel = Optional.absent();
    uiSchema = new UiSchema(false);
    uiSchema.add(Box.class, img.file);
  }

  @Override
  public void registerModelProvider(ModelProvider mp) {
    roadModel = Optional.fromNullable(mp.getModel(RoadModel.class));
    pdpModel = Optional.fromNullable(mp.getModel(PDPModel.class));
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    uiSchema.initialize(gc.getDevice());

    final Collection<Parcel> parcels = pdpModel.get().getParcels(
        ParcelState.values());
    final Image image = uiSchema.getImage(Box.class);
    checkState(image != null);

    synchronized (pdpModel.get()) {
      final Set<Vehicle> vehicles = pdpModel.get().getVehicles();
      final Map<Parcel, Vehicle> mapping = newLinkedHashMap();
      for (final Vehicle v : vehicles) {
        for (final Parcel p : pdpModel.get().getContents(v)) {
          mapping.put(p, v);
        }
        if (pdpModel.get().getVehicleState(v) != VehicleState.IDLE) {
          final PDPModel.VehicleParcelActionInfo vpai = pdpModel.get()
              .getVehicleActionInfo(v);
          mapping.put(vpai.getParcel(), vpai.getVehicle());
        }
      }

      for (final Parcel p : parcels) {
        float rotation = AT_SITE_ROTATION;
        int offsetX = 0;
        int offsetY = 0;
        @Nullable
        final ParcelState ps = pdpModel.get().getParcelState(p);
        if (ps == ParcelState.AVAILABLE) {
          final Point pos = roadModel.get().getPosition(p);
          final int x = vp.toCoordX(pos.x);
          final int y = vp.toCoordY(pos.y);
          offsetX = (int) img.atSiteOffset.x + x - image.getBounds().width / 2;
          offsetY = (int) img.atSiteOffset.y + y - image.getBounds().height / 2;
        } else if (ps == ParcelState.PICKING_UP || ps == ParcelState.DELIVERING) {

          final Vehicle v = mapping.get(p);
          final PDPModel.VehicleParcelActionInfo vpai = pdpModel.get()
              .getVehicleActionInfo(v);
          final Point pos = roadModel.get().getPosition(v);
          final int x = vp.toCoordX(pos.x);
          final int y = vp.toCoordY(pos.y);
          final double percentage = 1d - vpai.timeNeeded()
              / (double) p.getPickupDuration();
          final String text = ((int) (percentage * 100d)) + "%";

          final float rotFac = (float) (ps == ParcelState.PICKING_UP ? percentage
              : 1d - percentage);
          rotation = IN_CARGO_ROTATION * rotFac;

          final int textWidth = gc.textExtent(text).x;
          gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_BLUE));
          gc.drawText(text, (int) LABEL_OFFSET.x + x - textWidth / 2,
              (int) LABEL_OFFSET.y + y, true);

          Point from = new Point(img.atSiteOffset.x + x
              - image.getBounds().width
              / 2d, img.atSiteOffset.y + y - image.getBounds().height / 2d);
          Point to = new Point(img.inCargoOffset.x + x
              - image.getBounds().width
              / 2d, img.inCargoOffset.y + y - image.getBounds().height / 2d);

          if (ps == ParcelState.DELIVERING) {
            final Point temp = from;
            from = to;
            to = temp;
          }

          final Point diff = Point.diff(to, from);
          offsetX = (int) (from.x + (percentage * diff.x));
          offsetY = (int) (from.y + (percentage * diff.y));

        } else if (ps == ParcelState.IN_CARGO) {
          rotation = IN_CARGO_ROTATION;
          final Point pos = roadModel.get().getPosition(mapping.get(p));
          final int x = vp.toCoordX(pos.x);
          final int y = vp.toCoordY(pos.y);
          offsetX = (int) img.inCargoOffset.x + x - image.getBounds().width / 2;
          offsetY = (int) img.inCargoOffset.y + y - image.getBounds().height
              / 2;
        }

        if (ps != null && !ps.isDelivered()) {
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
    }
  }

  @Override
  public ViewRect getViewRect() {
    return null;
  }
}
