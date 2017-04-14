/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.core.model.road;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;

import com.github.rinde.rinsim.geom.Point;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public final class MapSpatialRegistry<T> implements SpatialRegistry<T> {

  volatile Map<T, Point> objLocs;

  private MapSpatialRegistry() {
    objLocs = Collections.synchronizedMap(new LinkedHashMap<T, Point>());
  }

  @Override
  public boolean containsObject(T object) {
    return objLocs.containsKey(object);
  }

  @Override
  public void removeObject(T object) {
    objLocs.remove(object);
  }

  @Override
  public void clear() {
    objLocs.clear();
  }

  @Override
  public Point getPosition(T object) {
    synchronized (objLocs) {
      checkArgument(containsObject(object), "RoadUser does not exist: %s.",
        object);
      return objLocs.get(object);
    }
  }

  @Override
  public void addAt(T object, Point position) {
    checkNotNull(position);
    objLocs.put(object, position);
  }

  @Override
  public ImmutableMap<T, Point> getObjectsAndPositions() {
    final ImmutableMap<T, Point> copy;
    synchronized (objLocs) {
      copy = ImmutableMap.copyOf(objLocs);
    }
    return copy;
  }

  @Override
  public ImmutableSet<T> getObjects() {
    final ImmutableSet<T> copy;
    synchronized (objLocs) {
      copy = ImmutableSet.copyOf(objLocs.keySet());
    }
    return copy;
  }

  // excludes objects on border of radius
  @Override
  public ImmutableSet<T> findObjectsWithinRadius(Point position,
      double radius) {
    checkArgument(radius > 0, "radius should be strictly positive, found %s.",
      radius);
    final ImmutableSet.Builder<T> builder = ImmutableSet.builder();
    synchronized (objLocs) {
      for (final Entry<T, Point> entry : objLocs.entrySet()) {
        if (Point.distance(position, entry.getValue()) < radius) {
          builder.add(entry.getKey());
        }
      }
    }
    return builder.build();
  }

  // include objects on border of rect
  @Override
  public ImmutableSet<T> findObjectsInRect(Point min, Point max) {
    checkArgument(min.x < max.x && min.y < max.y,
      "Invalid rectangle, expected 'min' < 'max', found %s and %s.", min, max);
    final ImmutableSet.Builder<T> builder = ImmutableSet.builder();
    synchronized (objLocs) {
      for (final Entry<T, Point> entry : objLocs.entrySet()) {
        if (isInRect(min, max, entry.getValue())) {
          builder.add(entry.getKey());
        }
      }
    }
    return builder.build();
  }

  // in case multiple objects with the same distance exist, the object that was
  // added to the registry first is prioritized
  @Override
  public ImmutableSet<T> findNearestObjects(Point position, int n) {
    checkArgument(n > 0, "n should be strictly positive, found %s.", n);
    synchronized (objLocs) {
      if (objLocs.isEmpty()) {
        return ImmutableSet.of();
      } else if (objLocs.size() <= n) {
        return getObjects();
      }

      final Queue<ObjDist<T>> queue = new PriorityQueue<>(n);
      for (final Entry<T, Point> entry : objLocs.entrySet()) {
        final double dist = Point.distance(position, entry.getValue());
        if (queue.size() < n) {
          queue.add(ObjDist.create(entry.getKey(), dist));
        } else if (queue.peek().dist() > dist) {
          queue.remove();
          queue.add(ObjDist.create(entry.getKey(), dist));
        }
      }

      final ImmutableSet.Builder<T> objs = ImmutableSet.builder();
      for (final ObjDist<T> od : queue) {
        objs.add(od.obj());
      }
      return objs.build();
    }
  }

  public static <T> SpatialRegistry<T> create() {
    return new MapSpatialRegistry<>();
  }

  static boolean isInRect(Point min, Point max, Point p) {
    return p.x >= min.x && p.x <= max.x && p.y >= min.y && p.y <= max.y;
  }

  @AutoValue
  abstract static class ObjDist<T> implements Comparable<ObjDist<T>> {

    abstract T obj();

    abstract double dist();

    @Override
    public int compareTo(ObjDist<T> other) {
      return Double.compare(other.dist(), dist());
    }

    static <T> ObjDist<T> create(T obj, double d) {
      return new AutoValue_MapSpatialRegistry_ObjDist<>(obj, d);
    }
  }
}
