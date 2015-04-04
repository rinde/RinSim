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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.AbstractModel;
import com.github.rinde.rinsim.core.ModelProvider;
import com.github.rinde.rinsim.core.ModelReceiver;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.google.common.collect.ImmutableList;

/**
 *
 * @author David Merckx
 * @author Rinde van Lon
 */
public class GradientModel extends AbstractModel<FieldEmitter> implements
  ModelReceiver {
  private final List<FieldEmitter> emitters;
  private double minX;
  private double maxX;
  private double minY;
  private double maxY;
  @Nullable
  private PDPModel pdpModel;

  GradientModel() {
    emitters = new CopyOnWriteArrayList<FieldEmitter>();
  }

  List<FieldEmitter> getEmitters() {
    return emitters;
  }

  List<Truck> getTruckEmitters() {
    final List<Truck> trucks = new ArrayList<Truck>();

    for (final FieldEmitter emitter : emitters) {
      if (emitter instanceof Truck) {
        trucks.add((Truck) emitter);
      }
    }

    return trucks;
  }

  /**
   * Possibilities (-1,1) (0,1) (1,1) (-1,0) (1,0 (-1,-1) (0,-1) (1,-1)
   */
  private static final int[] x = { -1, 0, 1, 1, 1, 0, -1, -1 };
  private static final int[] y = { 1, 1, 1, 0, -1, -1, -1, 0 };

  @Nullable
  Point getTargetFor(Truck element) {
    float maxField = Float.NEGATIVE_INFINITY;
    Point maxFieldPoint = null;

    for (int i = 0; i < x.length; i++) {
      final Point p = new Point(element.getPosition().x + x[i],
        element.getPosition().y + y[i]);

      if (p.x < minX || p.x > maxX || p.y < minY || p.y > maxY) {
        continue;
      }

      final float field = getField(p, element);
      if (field >= maxField) {
        maxField = field;
        maxFieldPoint = p;
      }
    }

    return maxFieldPoint;
  }

  float getField(Point in, Truck truck) {
    float field = 0.0f;
    for (final FieldEmitter emitter : emitters) {
      field += emitter.getStrength()
        / Point.distance(emitter.getPosition(), in);
    }

    for (final Parcel p : verifyNotNull(pdpModel).getContents(truck)) {
      field += 2 / Point.distance(p.getDestination(), in);
    }
    return field;
  }

  @Override
  public boolean register(FieldEmitter element) {
    emitters.add(element);
    element.setModel(this);
    return true;
  }

  @Override
  public boolean unregister(FieldEmitter element) {
    emitters.remove(element);
    return false;
  }

  Map<Point, Float> getFields(Truck truck) {
    final Map<Point, Float> fields = new HashMap<Point, Float>();

    for (int i = 0; i < x.length; i++) {
      final Point p = new Point(truck.getPosition().x + x[i],
        truck.getPosition().y + y[i]);

      if (p.x < minX || p.x > maxX || p.y < minY || p.y > maxY) {
        continue;
      }

      fields.put(new Point(x[i], y[i]), getField(p, truck));
    }

    float avg = 0;
    for (final Float f : fields.values()) {
      avg += f;
    }
    avg /= fields.size();
    for (final Entry<Point, Float> entry : fields.entrySet()) {
      fields.put(entry.getKey(), entry.getValue() - avg);
    }
    return fields;
  }

  @Override
  public void registerModelProvider(ModelProvider mp) {
    pdpModel = mp.tryGetModel(PDPModel.class);
    final ImmutableList<Point> bs = mp.getModel(RoadModel.class)
      .getBounds();

    minX = bs.get(0).x;
    maxX = bs.get(1).x;
    minY = bs.get(0).y;
    maxY = bs.get(1).y;
  }

  static StochasticSupplier<GradientModel> supplier() {
    return new StochasticSuppliers.AbstractStochasticSupplier<GradientModel>() {
      private static final long serialVersionUID = 1701618808844264668L;

      @Override
      public GradientModel get(long seed) {
        return new GradientModel();
      }
    };
  }
}
