package com.github.rinde.rinsim.examples.pdptw.gradientfield;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.graph.Point;
import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.ModelReceiver;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.github.rinde.rinsim.util.StochasticSuppliers.AbstractStochasticSupplier;
import com.google.common.collect.ImmutableList;

/**
 * 
 * @author David Merckx
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class GradientModel implements Model<FieldEmitter>, ModelReceiver {

  private final List<FieldEmitter> emitters;
  private double minX;
  private double maxX;
  private double minY;
  private double maxY;
  private PDPModel pdpModel;
  ImmutableList<Point> bounds;

  public GradientModel() {

    emitters = new CopyOnWriteArrayList<FieldEmitter>();
  }

  public List<FieldEmitter> getEmitters() {
    return emitters;
  }

  public List<Truck> getTruckEmitters() {
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
  private final int[] x = { -1, 0, 1, 1, 1, 0, -1, -1 };
  private final int[] y = { 1, 1, 1, 0, -1, -1, -1, 0 };

  @Nullable
  public Point getTargetFor(Truck element) {
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

  public float getField(Point in, Truck truck) {
    float field = 0.0f;
    for (final FieldEmitter emitter : emitters) {
      field += emitter.getStrength()
          / Point.distance(emitter.getPosition(), in);
    }

    for (final Parcel p : pdpModel.getContents(truck)) {
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

  @Override
  public Class<FieldEmitter> getSupportedType() {
    return FieldEmitter.class;
  }

  public Map<Point, Float> getFields(Truck truck) {
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
    for (final Point p : fields.keySet()) {
      avg += fields.get(p);
    }
    avg /= fields.size();
    for (final Point p : fields.keySet()) {
      fields.put(p, fields.get(p) - avg);
    }
    return fields;
  }

  @Override
  public void registerModelProvider(ModelProvider mp) {
    pdpModel = mp.getModel(PDPModel.class);
    final ImmutableList<Point> bounds = mp.getModel(RoadModel.class)
        .getBounds();

    minX = bounds.get(0).x;
    maxX = bounds.get(1).x;
    minY = bounds.get(0).y;
    maxY = bounds.get(1).y;
  }

  public static StochasticSupplier<GradientModel> supplier() {
    return new StochasticSuppliers.AbstractStochasticSupplier<GradientModel>() {
      @Override
      public GradientModel get(long seed) {
        return new GradientModel();
      }
    };
  }
}
