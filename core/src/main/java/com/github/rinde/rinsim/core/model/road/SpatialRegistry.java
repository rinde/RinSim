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

public interface SpatialRegistry {

  // idea:
  // wrap point in Container { pos, data }
  // data would only contain connection and rel pos etc
  // disadvantage: creating a container for every position, even if not needed
  // can this be done optionally?

  // disadvantage: no lookup by point, is needed for finding shortest path to
  // another RU, is it?

  // use a separate map for data: RU -> Data
  // RU -> Point
  // Point -> RU

  // use decorator to add extra data

  boolean containsObject(RoadUser object);

  void removeObject(RoadUser object);

  void clear();

  Point getPosition(RoadUser object);

  void addAt(RoadUser object, Point position);

  // creates snapshots
  ImmutableMap<RoadUser, Point> getObjectsAndPositions();

  ImmutableSet<RoadUser> getObjects();

}
