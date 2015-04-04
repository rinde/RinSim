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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;

import com.github.rinde.rinsim.core.ModelProvider;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.VehicleState;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;

/**
 * @author Rinde van Lon
 *
 */
public final class PDPModelRenderer implements ModelRenderer {

  private Color black;
  private Color white;
  private Color lightGray;
  private Color darkGreen;
  private Color green;
  private Color orange;
  private Color blue;
  private Color foregroundInfo;
  private Color backgroundInfo;

  private PDPModel pdpModel;
  private RoadModel roadModel;

  private boolean isInitialized;
  private final boolean drawDestLines;

  public PDPModelRenderer() {
    this(true);
  }

  public PDPModelRenderer(boolean drawDestinationLines) {
    drawDestLines = drawDestinationLines;
  }

  // TODO dispose colors on exit!
  private void initialize(GC gc) {
    isInitialized = true;
    black = gc.getDevice().getSystemColor(SWT.COLOR_BLACK);
    white = gc.getDevice().getSystemColor(SWT.COLOR_WHITE);
    darkGreen = gc.getDevice().getSystemColor(SWT.COLOR_DARK_GREEN);
    green = gc.getDevice().getSystemColor(SWT.COLOR_GREEN);
    blue = gc.getDevice().getSystemColor(SWT.COLOR_BLUE);

    lightGray = new Color(gc.getDevice(), 205, 201, 201);
    orange = new Color(gc.getDevice(), 255, 160, 0);

    foregroundInfo = white;
    backgroundInfo = blue;

  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    if (!isInitialized) {
      initialize(gc);
    }

    synchronized (pdpModel) {
      final Map<RoadUser, Point> posMap = roadModel.getObjectsAndPositions();
      final Set<Vehicle> vehicles = pdpModel.getVehicles();

      for (final Vehicle v : vehicles) {
        if (posMap.containsKey(v)) {
          final Point p = posMap.get(v);
          final double size = pdpModel.getContentsSize(v);

          final Collection<Parcel> contents = pdpModel.getContents(v);
          final int x = vp.toCoordX(p.x);
          final int y = vp.toCoordY(p.y);

          if (drawDestLines) {
            gc.setForeground(black);
            for (final Parcel parcel : contents) {
              final Point po = parcel.getDestination();
              final int xd = vp.toCoordX(po.x);
              final int yd = vp.toCoordY(po.y);
              if (parcel.getDeliveryTimeWindow().isBeforeStart(time)) {
                gc.setBackground(darkGreen);
              } else if (parcel.getDeliveryTimeWindow().isBeforeEnd(time)) {
                gc.setBackground(green);
              } else {
                gc.setBackground(orange);
              }
              gc.drawLine(x, y, xd, yd);
              gc.fillOval(xd - 5, yd - 5, 10, 10);
              gc.drawOval(xd - 5, yd - 5, 10, 10);
            }
          }
          gc.setBackground(backgroundInfo);
          gc.setForeground(foregroundInfo);
          final VehicleState state = pdpModel.getVehicleState(v);
          // FIXME, investigate why the second check is
          // neccesary..
          if (state != VehicleState.IDLE
            && pdpModel.getVehicleActionInfo(v) != null) {
            gc.drawText(
              state.toString() + " "
                + pdpModel.getVehicleActionInfo(v).timeNeeded(), x, y - 20);
          }
          gc.drawText(Double.toString(size), x, y);
          drawMore(gc, vp, time, v, p, posMap);
        }
      }

      final Collection<Parcel> parcels = pdpModel.getParcels(
        ParcelState.AVAILABLE, ParcelState.ANNOUNCED);
      for (final Parcel parcel : parcels) {

        final Point p = posMap.get(parcel);
        if (posMap.containsKey(parcel)) {
          final int x = vp.toCoordX(p.x);
          final int y = vp.toCoordY(p.y);
          gc.setForeground(lightGray);
          gc.drawLine(x, y, vp.toCoordX(parcel.getDestination().x),
            vp.toCoordY(parcel.getDestination().y));

          if (parcel.getPickupTimeWindow().isBeforeStart(time)) {
            gc.setBackground(darkGreen);
          } else if (parcel.getPickupTimeWindow().isBeforeEnd(time)) {
            gc.setBackground(green);
          } else {
            gc.setBackground(orange);
          }
          gc.setForeground(black);
          gc.fillOval(x - 5, y - 5, 10, 10);
        }
      }
    }
  }

  protected void drawMore(GC gc, ViewPort vp, long time, Vehicle v, Point p,
    Map<RoadUser, Point> posMap) {}

  @Nullable
  @Override
  public ViewRect getViewRect() {
    return null;
  }

  @Override
  public void registerModelProvider(ModelProvider mp) {
    pdpModel = mp.tryGetModel(PDPModel.class);
    roadModel = mp.tryGetModel(RoadModel.class);
  }

}
