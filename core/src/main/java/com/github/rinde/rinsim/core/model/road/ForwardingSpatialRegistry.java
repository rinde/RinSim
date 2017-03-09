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

import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public abstract class ForwardingSpatialRegistry implements SpatialRegistry {

  abstract SpatialRegistry delegate();

  @Override
  public boolean containsObject(RoadUser object) {
    return delegate().containsObject(object);
  }

  @Override
  public void removeObject(RoadUser object) {
    delegate().removeObject(object);
  }

  @Override
  public void clear() {
    delegate().clear();
  }

  @Override
  public Point getPosition(RoadUser object) {
    return delegate().getPosition(object);
  }

  @Override
  public void addAt(RoadUser object, Point position) {
    delegate().addAt(object, position);
  }

  @Override
  public ImmutableMap<RoadUser, Point> getObjectsAndPositions() {
    return delegate().getObjectsAndPositions();
  }

  @Override
  public ImmutableSet<RoadUser> getObjects() {
    return delegate().getObjects();
  }
}
