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

import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public final class MapSpatialRegistry implements SpatialRegistry {

  volatile Map<RoadUser, Point> objLocs;

  MapSpatialRegistry() {
    objLocs = Collections.synchronizedMap(new LinkedHashMap<RoadUser, Point>());
  }

  @Override
  public boolean containsObject(RoadUser object) {
    return objLocs.containsKey(object);
  }

  @Override
  public void removeObject(RoadUser object) {
    objLocs.remove(object);
  }

  @Override
  public void clear() {
    objLocs.clear();
  }

  @Override
  public Point getPosition(RoadUser object) {
    synchronized (objLocs) {
      checkArgument(containsObject(object), "RoadUser does not exist: %s.",
        object);
      return objLocs.get(object);
    }
  }

  @Override
  public void addAt(RoadUser object, Point position) {
    checkNotNull(position);
    objLocs.put(object, position);
  }

  @Override
  public ImmutableMap<RoadUser, Point> getObjectsAndPositions() {
    final ImmutableMap<RoadUser, Point> copy;
    synchronized (objLocs) {
      copy = ImmutableMap.copyOf(objLocs);
    }
    return copy;
  }

  @Override
  public ImmutableSet<RoadUser> getObjects() {
    final ImmutableSet<RoadUser> copy;
    synchronized (objLocs) {
      copy = ImmutableSet.copyOf(objLocs.keySet());
    }
    return copy;
  }
}
